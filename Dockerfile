FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

COPY .mvn/ .mvn/

COPY mvnw pom.xml ./

RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

COPY src/ src/

RUN ./mvnw package -DskipTests -q

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

USER appuser

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

