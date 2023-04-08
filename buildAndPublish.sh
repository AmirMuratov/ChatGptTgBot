./gradlew shadowJar
docker buildx build --push --platform linux/amd64,linux/arm64/v8 -t amirmuratov/chatgpt-tg-bot:latest .
