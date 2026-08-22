from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    DATABASE_URL: str = "postgresql://postgres:password123@localhost:5432/streetfood_ai"
    REDIS_URL: str = "redis://localhost:6379/0"
    SECRET_KEY: str = "supersecretjwtkey12345678901234567890"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 1440  # 1 day for hackathon convenience
    GEMINI_API_KEY: str = ""

    class Config:
        env_file = ".env"
        extra = "ignore"

settings = Settings()
