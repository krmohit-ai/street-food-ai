from sqlalchemy import create_engine
from sqlalchemy.orm import declarative_base, sessionmaker
import redis
from app.config import settings

# 1. PostgreSQL DB Config
engine = create_engine(
    settings.DATABASE_URL,
    pool_pre_ping=True,  # checks connection health before issuing queries
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# 2. Redis Cache Config
redis_client = redis.from_url(settings.REDIS_URL, decode_responses=True)

def get_redis():
    try:
        yield redis_client
    finally:
        pass
