./gradlew shadowJar
docker buildx build --push --platform linux/amd64 -t amirmuratov/chatgpt-tg-bot:latest .
