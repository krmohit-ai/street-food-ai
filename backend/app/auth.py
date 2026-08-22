from datetime import datetime, timedelta
from typing import Optional
from jose import JWTError, jwt
from google.oauth2 import id_token
from google.auth.transport import requests as google_requests
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy.orm import Session
from app.config import settings
from app.database import get_db
from app import models

# OAuth2 scheme pointing to our Google Auth endpoint
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="api/auth/google")

def verify_google_token(token: str) -> dict:
    # Hackathon Dev Bypass: Support mock tokens to ease local testing
    if "mock_momo" in token:
        return {"email": "momo.vendor@gmail.com", "iss": "accounts.google.com"}
    elif "mock_chai" in token:
        return {"email": "chai.vendor@gmail.com", "iss": "accounts.google.com"}
    elif "mock_customer" in token:
        return {"email": "test_customer@gmail.com", "iss": "accounts.google.com"}

    try:
        # Verify the Google ID Token. audience=None allows verification
        # without hardcoding specific Client IDs, suitable for hackathons/multiple clients.
        idinfo = id_token.verify_oauth2_token(token, google_requests.Request(), audience=None)
        
        # Verify issuer is Google
        if idinfo.get('iss') not in ['accounts.google.com', 'https://accounts.google.com']:
            raise ValueError('Wrong issuer.')
            
        return idinfo
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=f"Google ID Token verification failed: {e}",
            headers={"WWW-Authenticate": "Bearer"},
        )

def create_access_token(data: dict, expires_delta: Optional[timedelta] = None) -> str:
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)
    return encoded_jwt

def get_current_user(token: str = Depends(oauth2_scheme), db: Session = Depends(get_db)) -> models.User:
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])
        user_id: str = payload.get("sub")
        if user_id is None:
            raise credentials_exception
    except JWTError:
        raise credentials_exception
        
    user = db.query(models.User).filter(models.User.id == user_id).first()
    if user is None:
        raise credentials_exception
    return user

def get_current_vendor(current_user: models.User = Depends(get_current_user)) -> models.User:
    if current_user.role != "vendor":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Operation restricted to vendors only."
        )
    return current_user
