from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "naeda-ai"
    app_host: str = "0.0.0.0"
    app_port: int = 8000
    app_reload: bool = True

    database_url: str = "postgresql+psycopg2://postgres:postgres@localhost:5432/naeda_ai"

    # ArcFace model name used by InsightFace (e.g., buffalo_l)
    arcface_model_name: str = "buffalo_l"
    # CPUExecutionProvider for local default; can be changed to CUDAExecutionProvider
    arcface_provider: str = "CPUExecutionProvider"

    # Similarity threshold for match decision
    similarity_threshold: float = 0.7

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
