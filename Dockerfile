# --- Gradle 빌드 단계 ---
FROM gradle:8.10.2-jdk17 AS build
WORKDIR /app

# submodule 코드 복사
COPY . /app

RUN gradle clean build -x test

# --- 실행 단계 ---
FROM eclipse-temurin:17-jdk
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

# Infisical CLI 설치
RUN apt-get update && apt-get install -y curl gnupg \
 && curl -1sLf 'https://artifacts-cli.infisical.com/setup.deb.sh' | bash \
 && apt-get update && apt-get install -y infisical \
 && rm -rf /var/lib/apt/lists/*

ENV INFISICAL_DISABLE_UPDATE_CHECK=true

EXPOSE 8080

CMD ["infisical", "run", "--", "java", "-jar", "app.jar"]
# 필요 시 프로파일 지정: , "--spring.profiles.active=test"]
