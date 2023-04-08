FROM alpine:3.17.3

RUN apk add openjdk17
RUN apk add ffmpeg

WORKDIR /app
COPY build/libs/*.jar app.jar
CMD ["java", "-jar", "app.jar"]