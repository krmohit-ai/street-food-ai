from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.database import get_db
from app import models, schemas, auth

router = APIRouter(prefix="/auth", tags=["Authentication"])

@router.post("/register", response_model=schemas.UserResponse, status_code=status.HTTP_201_CREATED)
def register(user_data: schemas.UserCreate, db: Session = Depends(get_db)):
    # 1. Check if user already exists
    db_user = db.query(models.User).filter(models.User.phone == user_data.phone).first()
    if db_user:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Phone number already registered."
        )

    # 2. Check vendor constraints
    if user_data.role == "vendor" and not user_data.business_name:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Business name is required for vendor accounts."
        )

    # 3. Create User record
    hashed_pwd = auth.get_password_hash(user_data.password)
    new_user = models.User(
        phone=user_data.phone,
        password_hash=hashed_pwd,
        role=user_data.role
    )
    db.add(new_user)
    db.flush()  # gets the user.id generated

    # 4. Create VendorProfile if vendor
    if user_data.role == "vendor":
        new_profile = models.VendorProfile(
            id=new_user.id,
            business_name=user_data.business_name,
            is_active=False,
            status="closed",
            rating=5.00
        )
        db.add(new_profile)

    db.commit()
    db.refresh(new_user)
    return new_user

@router.post("/login", response_model=schemas.Token)
def login(login_data: schemas.UserLogin, db: Session = Depends(get_db)):
    # 1. Look up user
    user = db.query(models.User).filter(models.User.phone == login_data.phone).first()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid phone number or password."
        )

    # 2. Check password
    if not auth.verify_password(login_data.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid phone number or password."
        )

    # 3. Generate Token
    access_token = auth.create_access_token(data={"sub": str(user.id)})
    
    # 4. Prepare TokenUser structure
    token_user = schemas.TokenUser(
        id=user.id,
        phone=user.phone,
        role=user.role
    )

    return schemas.Token(
        access_token=access_token,
        token_type="bearer",
        user=token_user
    )
