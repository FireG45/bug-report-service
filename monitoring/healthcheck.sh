#!/bin/bash

# Long-running host-side monitor. State is kept in memory, so it must run as a
# service (monitoring/bug-report-monitor.service), not from cron.

# === Configuration ===
ENV_FILE="${ENV_FILE:-/root/bug-report-service/.env}"
CHECK_INTERVAL_MIN="${CHECK_INTERVAL:-3}"
CHECK_INTERVAL=$((CHECK_INTERVAL_MIN * 60))

ACCESS_LOG="${ACCESS_LOG:-/var/log/nginx/access.log}"
BACKUP_DIR="${BACKUP_DIR:-/root/backups/postgres}"

# api.telegram.org resolves to 149.154.166.110 on this host; that DC is
# blackholed by the provider, while 149.154.167.220 works. Empty = plain DNS.
TELEGRAM_API_IP="${TELEGRAM_API_IP:-149.154.167.220}"
BACKUP_MAX_AGE_MIN="${BACKUP_MAX_AGE_MIN:-1560}"   # 26h
DISK_THRESHOLD="${DISK_THRESHOLD:-85}"
METRICS_WINDOW_MIN="${METRICS_WINDOW_MIN:-5}"
FIVEXX_THRESHOLD="${FIVEXX_THRESHOLD:-5}"
TOOMANY_THRESHOLD="${TOOMANY_THRESHOLD:-50}"
UNAUTH_THRESHOLD="${UNAUTH_THRESHOLD:-30}"

CONTAINERS=(report-service report-postgres frontend)

# name|url — expected HTTP 200
SERVICES=(
  "report-service|http://127.0.0.1:8080/healthcheck/readiness"
  "site|https://mrak-bre.ru/"
  "frontend-report-api|https://mrak-bre.ru/api/get-frontend-report"
)

declare -A PREV_STATUS
CONTAINERS_ALERTED=0
DISK_ALERTED=0
BACKUP_ALERTED=0
FIVEXX_ALERTED=0
TOOMANY_ALERTED=0
UNAUTH_ALERTED=0

read_env() {
  local key="$1" value="${!1:-}"
  if [ -n "$value" ]; then printf '%s' "$value"; return; fi
  [ -r "$ENV_FILE" ] || return 0
  sed -n "s/^${key}=//p" "$ENV_FILE" | tail -n1 | sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'$//"
}

BOT_TOKEN="$(read_env TELEGRAM_BOT_TOKEN)"
CHAT_ID="$(read_env TELEGRAM_CHAT_ID)"

send_telegram() {
  local message="$1" response
  if [ -z "$BOT_TOKEN" ] || [ -z "$CHAT_ID" ]; then
    echo "WARN: TELEGRAM_BOT_TOKEN/TELEGRAM_CHAT_ID not set, alert not sent"
    return
  fi

  local url="https://api.telegram.org/bot${BOT_TOKEN}/sendMessage"

  if [ -n "$TELEGRAM_API_IP" ]; then
    response=$(curl -sS --max-time 15 --resolve "api.telegram.org:443:${TELEGRAM_API_IP}" \
      -X POST "$url" \
      -d chat_id="${CHAT_ID}" \
      --data-urlencode text="${message}" \
      -d parse_mode="HTML" 2>&1)
    if [[ "$response" == *'"ok":true'* ]]; then return 0; fi
    echo "[$(date '+%F %T')] WARN: telegram via ${TELEGRAM_API_IP} failed: ${response:0:200}"
  fi

  response=$(curl -sS --max-time 15 -X POST "$url" \
    -d chat_id="${CHAT_ID}" \
    --data-urlencode text="${message}" \
    -d parse_mode="HTML" 2>&1)
  if [[ "$response" == *'"ok":true'* ]]; then return 0; fi
  echo "[$(date '+%F %T')] WARN: telegram send failed: ${response:0:200}"
}

http_code() {
  curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 --max-time 10 "$1"
}

container_problems() {
  local name state running health problems=()
  for name in "${CONTAINERS[@]}"; do
    if ! state=$(docker inspect -f '{{.State.Running}} {{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$name" 2>/dev/null); then
      problems+=("${name}: not found")
      continue
    fi
    running="${state%% *}"
    health="${state#* }"
    if [ "$running" != "true" ]; then
      problems+=("${name}: stopped")
    elif [ "$health" = "unhealthy" ]; then
      problems+=("${name}: unhealthy")
    fi
  done
  printf '%s' "${problems[*]}"
  [ ${#problems[@]} -eq 0 ]
}

disk_used_percent() {
  df -P / | awk 'NR==2 {gsub(/%/, "", $5); print $5}'
}

backup_age_min() {
  local newest
  newest=$(find "$BACKUP_DIR" -maxdepth 1 -type f -name '*.dump*' -printf '%T@\n' 2>/dev/null | sort -rn | head -n1)
  if [ -z "$newest" ]; then echo -1; return; fi
  echo $(( ($(date +%s) - ${newest%.*}) / 60 ))
}

# Prints: total 5xx 429 401 for the trailing window.
count_statuses() {
  local cutoff
  cutoff=$(date -u -d "-$1 minutes" '+%Y%m%d%H%M')
  tail -n 50000 "$ACCESS_LOG" 2>/dev/null | awk -v cutoff="$cutoff" '
    BEGIN {
      split("Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec", months, " ")
      for (i = 1; i <= 12; i++) month[months[i]] = sprintf("%02d", i)
    }
    match($0, /\[[0-9][0-9]\/[A-Za-z][A-Za-z][A-Za-z]\/[0-9][0-9][0-9][0-9]:[0-9][0-9]:[0-9][0-9]:[0-9][0-9]/) {
      ts = substr($0, RSTART + 1, RLENGTH - 1)
      key = substr(ts, 8, 4) month[substr(ts, 4, 3)] substr(ts, 1, 2) substr(ts, 13, 2) substr(ts, 16, 2)
      if (key > cutoff) {
        total++
        if ($0 ~ /" 5[0-9][0-9] /) fivexx++
        else if ($0 ~ /" 429 /) too_many++
        else if ($0 ~ /" 401 /) unauthorized++
      }
    }
    END { printf "%d %d %d %d\n", total, fivexx, too_many, unauthorized }'
}

echo "=== Health Check Monitor ==="
echo "Interval: ${CHECK_INTERVAL_MIN}m"
echo "Services: ${#SERVICES[@]}"
echo ""

send_telegram "<b>🟢 Health Monitor started</b>
Checking ${#SERVICES[@]} services and ${#CONTAINERS[@]} containers every ${CHECK_INTERVAL_MIN}m
Metrics window: ${METRICS_WINDOW_MIN}m (5xx ≥ ${FIVEXX_THRESHOLD}, 429 ≥ ${TOOMANY_THRESHOLD}, 401 ≥ ${UNAUTH_THRESHOLD})"

while true; do
  for entry in "${SERVICES[@]}"; do
    IFS='|' read -r name target <<< "$entry"
    code=$(http_code "$target")

    if [ "$code" = "200" ]; then status="up"; else status="down"; fi
    prev="${PREV_STATUS[$name]:-up}"

    if [ "$status" = "down" ] && [ "$prev" != "down" ]; then
      echo "[$(date '+%F %T')] ALERT: ${name} is DOWN (HTTP ${code})"
      send_telegram "<b>🔴 ${name} is DOWN</b>
Target: <code>${target}</code>
HTTP: ${code}
Time: $(date -u '+%F %T UTC')"
    elif [ "$status" = "up" ] && [ "$prev" = "down" ]; then
      echo "[$(date '+%F %T')] RECOVERED: ${name} is UP"
      send_telegram "<b>🟢 ${name} RECOVERED</b>
Target: <code>${target}</code>
Time: $(date -u '+%F %T UTC')"
    fi

    PREV_STATUS[$name]="$status"
  done

  if problems=$(container_problems); then
    if [ "$CONTAINERS_ALERTED" = 1 ]; then
      CONTAINERS_ALERTED=0
      send_telegram "<b>🟢 Containers recovered</b>
Time: $(date -u '+%F %T UTC')"
    fi
  elif [ "$CONTAINERS_ALERTED" = 0 ]; then
    CONTAINERS_ALERTED=1
    echo "[$(date '+%F %T')] ALERT: containers: ${problems}"
    send_telegram "<b>🔴 Container problem</b>
<code>${problems}</code>
Time: $(date -u '+%F %T UTC')"
  fi

  used=$(disk_used_percent)
  if [ "$used" -ge "$DISK_THRESHOLD" ] && [ "$DISK_ALERTED" = 0 ]; then
    DISK_ALERTED=1
    echo "[$(date '+%F %T')] ALERT: disk ${used}%"
    send_telegram "<b>⚠️ Disk usage ${used}%</b>
Threshold: ${DISK_THRESHOLD}%
Time: $(date -u '+%F %T UTC')"
  elif [ "$used" -lt $((DISK_THRESHOLD - 5)) ] && [ "$DISK_ALERTED" = 1 ]; then
    DISK_ALERTED=0
    send_telegram "<b>🟢 Disk usage back to ${used}%</b>
Time: $(date -u '+%F %T UTC')"
  fi

  age=$(backup_age_min)
  if [ "$age" -lt 0 ] || [ "$age" -gt "$BACKUP_MAX_AGE_MIN" ]; then
    if [ "$BACKUP_ALERTED" = 0 ]; then
      BACKUP_ALERTED=1
      echo "[$(date '+%F %T')] ALERT: backup age ${age}m"
      send_telegram "<b>⚠️ Database backup missing or stale</b>
Latest backup age: ${age}m (limit ${BACKUP_MAX_AGE_MIN}m)
Dir: <code>${BACKUP_DIR}</code>
Time: $(date -u '+%F %T UTC')"
    fi
  elif [ "$BACKUP_ALERTED" = 1 ]; then
    BACKUP_ALERTED=0
    send_telegram "<b>🟢 Database backup is fresh again (${age}m)</b>
Time: $(date -u '+%F %T UTC')"
  fi

  read -r total f5 t429 t401 < <(count_statuses "$METRICS_WINDOW_MIN")

  if [ "$f5" -ge "$FIVEXX_THRESHOLD" ]; then
    if [ "$FIVEXX_ALERTED" = 0 ]; then
      FIVEXX_ALERTED=1
      echo "[$(date '+%F %T')] ALERT: ${f5} 5xx in ${METRICS_WINDOW_MIN}m"
      send_telegram "<b>⚠️ 5xx spike</b>
5xx in ${METRICS_WINDOW_MIN}m: <b>${f5}</b> (threshold ${FIVEXX_THRESHOLD})
Total requests: ${total}
Time: $(date -u '+%F %T UTC')"
    fi
  elif [ "$f5" -lt $((FIVEXX_THRESHOLD / 2)) ] && [ "$FIVEXX_ALERTED" = 1 ]; then
    FIVEXX_ALERTED=0
    send_telegram "<b>🟢 5xx back to normal (${f5} in ${METRICS_WINDOW_MIN}m)</b>
Time: $(date -u '+%F %T UTC')"
  fi

  if [ "$t429" -ge "$TOOMANY_THRESHOLD" ]; then
    if [ "$TOOMANY_ALERTED" = 0 ]; then
      TOOMANY_ALERTED=1
      echo "[$(date '+%F %T')] ALERT: ${t429} 429 in ${METRICS_WINDOW_MIN}m"
      send_telegram "<b>⚠️ 429 spike</b>
429 in ${METRICS_WINDOW_MIN}m: <b>${t429}</b> (threshold ${TOOMANY_THRESHOLD})
Total requests: ${total}
Time: $(date -u '+%F %T UTC')"
    fi
  elif [ "$t429" -lt $((TOOMANY_THRESHOLD / 2)) ] && [ "$TOOMANY_ALERTED" = 1 ]; then
    TOOMANY_ALERTED=0
    send_telegram "<b>🟢 429 back to normal (${t429} in ${METRICS_WINDOW_MIN}m)</b>
Time: $(date -u '+%F %T UTC')"
  fi

  if [ "$t401" -ge "$UNAUTH_THRESHOLD" ]; then
    if [ "$UNAUTH_ALERTED" = 0 ]; then
      UNAUTH_ALERTED=1
      echo "[$(date '+%F %T')] ALERT: ${t401} 401 in ${METRICS_WINDOW_MIN}m"
      send_telegram "<b>⚠️ 401 spike</b>
401 in ${METRICS_WINDOW_MIN}m: <b>${t401}</b> (threshold ${UNAUTH_THRESHOLD})
Total requests: ${total}
Time: $(date -u '+%F %T UTC')"
    fi
  elif [ "$t401" -lt $((UNAUTH_THRESHOLD / 2)) ] && [ "$UNAUTH_ALERTED" = 1 ]; then
    UNAUTH_ALERTED=0
    send_telegram "<b>🟢 401 back to normal (${t401} in ${METRICS_WINDOW_MIN}m)</b>
Time: $(date -u '+%F %T UTC')"
  fi

  sleep "$CHECK_INTERVAL"
done
