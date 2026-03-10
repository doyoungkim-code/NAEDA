from functools import lru_cache
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "naeda-ai-internal"
    app_host: str = "0.0.0.0"
    app_port: int = 8000
    app_reload: bool = True

    arcface_model_name: str = "buffalo_l"
    arcface_provider: str = "CPUExecutionProvider"
    arcface_det_size: int = 320
    ai_timeout_seconds: float = 5.0
    ai_retry_count: int = 1
    ai_retry_backoff_ms: int = 150
    ai_max_image_bytes: int = 3_145_728
    ai_feature_version: str = "fds-feature-v1"
    ai_rule_version: str = "fds-rule-v1"
    ai_model_version: str = "rule-only-v1"
    ai_model_algorithm: str = "RULE_ENGINE"
    ai_model_artifact_path: str = "N/A"
    ai_model_updated_at: str = "2026-03-09T00:00:00"
    resident_ocr_provider: str = "disabled"
    resident_ocr_mock_document_type: str = "RESIDENT_ID"
    resident_ocr_mock_name: str = "홍길동"
    resident_ocr_mock_front6: str = "900101"
    resident_ocr_mock_back1: str = "1"
    resident_ocr_mock_confidence: float = 0.95
    resident_ocr_paddle_lang: str = "korean"
    resident_ocr_min_confidence: float = 0.5
    resident_ocr_paddle_home: str = str(Path(__file__).resolve().parents[2] / ".paddle")

    internal_service_token: str = "dev-internal-token"

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
