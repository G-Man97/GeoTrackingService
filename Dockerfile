FROM alpine:3.23.3 AS builder

RUN apk add openjdk25

WORKDIR /app

COPY build.gradle gradlew ./
COPY gradle gradle
COPY src src
RUN ./gradlew clean build --no-daemon


FROM alpine:3.23.3 AS result

RUN apk add openjdk25

WORKDIR /app

COPY --from=builder /app/build/libs/app-1.0.jar application.jar
COPY src/main/resources/application-docker.yml application-docker.yml

ENTRYPOINT ["sh", "-c", "java \
  -Dspring.profiles.active=docker \
  -Dspring.datasource.url=${DB_URL} \
  -Dspring.datasource.username=${DB_USERNAME} \
  -Dspring.datasource.password=${DB_PASSWORD} \
  -jar /app/application.jar"]