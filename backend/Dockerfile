FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
RUN ./gradlew dependencies --no-daemon
COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S tikinti && adduser -S tikinti -G tikinti
COPY --from=builder /app/build/libs/*.jar app.jar
RUN mkdir -p /app/uploads && chown tikinti:tikinti /app/uploads
USER tikinti
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
