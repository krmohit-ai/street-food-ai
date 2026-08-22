from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.database import get_db
from app import models, schemas, auth

router = APIRouter(prefix="/auth", tags=["Authentication"])

@router.post("/google", response_model=schemas.Token)
def google_login(login_data: schemas.GoogleLoginRequest, db: Session = Depends(get_db)):
    # 1. Verify the Google ID Token
    idinfo = auth.verify_google_token(login_data.id_token)
    email = idinfo.get("email")
    if not email:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Google ID token does not contain email."
        )

    # 2. Check if user already exists
    user = db.query(models.User).filter(models.User.email == email).first()
    
    if not user:
        # If user does not exist, they must provide role to register
        if not login_data.role:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="User account does not exist. Specify 'role' to register."
            )
        if login_data.role not in ["vendor", "customer"]:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid role. Must be 'vendor' or 'customer'."
            )
        if login_data.role == "vendor" and not login_data.business_name:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Business name is required for vendor registration."
            )

        # Create new User
        user = models.User(
            email=email,
            role=login_data.role
        )
        db.add(user)
        db.flush()

        # Create VendorProfile if vendor
        if login_data.role == "vendor":
            new_profile = models.VendorProfile(
                id=user.id,
                business_name=login_data.business_name,
                is_active=False,
                status="closed",
                rating=5.00
            )
            db.add(new_profile)

        db.commit()
        db.refresh(user)

    # 3. Generate Backend JWT
    access_token = auth.create_access_token(data={"sub": str(user.id)})
    
    # 4. Prepare TokenUser structure
    token_user = schemas.TokenUser(
        id=user.id,
        email=user.email,
        role=user.role
    )

    return schemas.Token(
        access_token=access_token,
        token_type="bearer",
        user=token_user
    )
