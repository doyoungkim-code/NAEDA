# ============================================
# Stage 1: Build
# ============================================
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Gradle 설정 파일 먼저 복사 (의존성 캐싱)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

# 소스코드 복사 후 빌드
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# ============================================
# Stage 2: Run
# ============================================
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
