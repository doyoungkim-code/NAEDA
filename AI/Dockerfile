FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    APP_HOST=0.0.0.0 \
    APP_PORT=8000 \
    APP_RELOAD=false

WORKDIR /app

# 시스템 의존성 설치
# build-essential: insightface C++ 빌드에 필요 (g++, gcc 포함)
# curl: healthcheck용
RUN apt-get update --fix-missing \
    && apt-get install -y --no-install-recommends --fix-missing \
        libgl1 \
        libglib2.0-0 \
        curl \
        build-essential \
    && rm -rf /var/lib/apt/lists/*

# 패키지 먼저 복사 (레이어 캐싱 최적화 - 코드 변경 시 pip 재설치 방지)
COPY requirements.txt .
RUN pip install -r requirements.txt

# 소스 복사
COPY app ./app
COPY openapi_internal_embeddings.yaml ./openapi_internal_embeddings.yaml
COPY README.md ./README.md

EXPOSE 8000

# Healthcheck (컨테이너 자체 상태 확인)
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8000/health || exit 1

CMD ["python", "-m", "app.run"]
