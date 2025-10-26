FROM eclipse-temurin:21-alpine as builder
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src ./src
RUN ./mvnw clean install

FROM eclipse-temurin:21-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar /app/*.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/*.jar"]