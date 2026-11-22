#!/bin/bash

# === Configuration ===
BOT_TOKEN="${TELEGRAM_BOT_TOKEN}"
CHAT_ID="${TELEGRAM_CHAT_ID}"
CHECK_INTERVAL_MIN="${CHECK_INTERVAL:-3}"
CHECK_INTERVAL=$((CHECK_INTERVAL_MIN * 60))

# Services: name|url
SERVICES=(
  "report-service|http://report-service:8080/healthcheck"
)

# Track previous status to avoid alert spam
declare -A PREV_STATUS

send_telegram() {
  local message="$1"
  curl -s -X POST "https://api.telegram.org/bot${BOT_TOKEN}/sendMessage" \
    -d chat_id="${CHAT_ID}" \
    -d text="${message}" \
    -d parse_mode="HTML" > /dev/null 2>&1
}

check_http() {
  local url="$1"
  local http_code
  http_code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 --max-time 10 "$url")
  [ "$http_code" = "200" ]
}

echo "=== Health Check Monitor ==="
echo "Interval: ${CHECK_INTERVAL_MIN}m"
echo "Services: ${#SERVICES[@]}"
echo ""

# Send startup message
send_telegram "$(cat <<EOF
<b>🟢 Health Monitor started</b>
Checking ${#SERVICES[@]} services every ${CHECK_INTERVAL_MIN}m
EOF
)"

while true; do
  for entry in "${SERVICES[@]}"; do
    IFS='|' read -r name target <<< "$entry"

    if check_http "$target"; then
      status="up"
    else
      status="down"
    fi

    prev="${PREV_STATUS[$name]:-up}"

    # Alert only on status change
    if [ "$status" = "down" ] && [ "$prev" != "down" ]; then
      echo "[$(date '+%H:%M:%S')] ALERT: ${name} is DOWN"
      send_telegram "$(cat <<EOF
<b>🔴 ${name} is DOWN</b>
Target: <code>${target}</code>
Time: $(date '+%Y-%m-%d %H:%M:%S')
EOF
)"
    elif [ "$status" = "up" ] && [ "$prev" = "down" ]; then
      echo "[$(date '+%H:%M:%S')] RECOVERED: ${name} is UP"
      send_telegram "$(cat <<EOF
<b>🟢 ${name} RECOVERED</b>
Target: <code>${target}</code>
Time: $(date '+%Y-%m-%d %H:%M:%S')
EOF
)"
    fi

    PREV_STATUS[$name]="$status"
  done

  sleep "$CHECK_INTERVAL"
done
