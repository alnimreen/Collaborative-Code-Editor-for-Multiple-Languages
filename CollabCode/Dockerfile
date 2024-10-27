FROM openjdk:17-jdk-slim

WORKDIR /app
RUN apt-get update && apt-get install -y \
    curl \
    && curl -fsSL https://get.docker.com/ | sh \
    && rm -rf /var/lib/apt/lists/*

ENV PATH="/usr/local/bin:${PATH}"

COPY target/CollabCode-0.0.1-SNAPSHOT.jar /app/collabcode-backend.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/collabcode-backend.jar"]
