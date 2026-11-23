FROM eclipse-temurin:21-alpine as builder
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src ./src
RUN chmod +x mvnw && ./mvnw clean install

FROM eclipse-temurin:21-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S -G app app
COPY --from=builder /app/target/*.jar /app/*.jar
USER app

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/*.jar"]