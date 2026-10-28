# ---------- build stage ----------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Gradle wrapper and build files first: this layer is cached until they change.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
# Windows checkouts can give gradlew CRLF line endings, which breaks it on Linux.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null

COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# ---------- run stage ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build /workspace/build/libs/app.jar app.jar
USER app

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8081
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]