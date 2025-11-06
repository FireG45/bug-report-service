# ====== Stage 1: Build ======
FROM gradle:8.8-jdk21 AS builder
WORKDIR /app

COPY build.gradle settings.gradle gradle.* ./
RUN gradle build --no-daemon -x test || true

COPY . .
RUN gradle clean bootJar --no-daemon --stacktrace

# ====== Stage 2: Runtime ======
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

VOLUME ["/models"]
EXPOSE 8083

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/*.jar"]