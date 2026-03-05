from functools import lru_cache

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

    internal_service_token: str = "dev-internal-token"

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
