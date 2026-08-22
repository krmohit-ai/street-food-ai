import uuid
from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from redis import Redis
from app.database import get_db, get_redis
from app import models, schemas, auth

router = APIRouter(prefix="/vendor", tags=["Vendor Operations"])

# --- Menu Management ---

@router.get("/menu", response_model=list[schemas.ProductResponse])
def get_menu(
    current_vendor: models.User = Depends(auth.get_current_vendor),
    db: Session = Depends(get_db)
):
    products = db.query(models.Product).filter(
        models.Product.vendor_profile_id == current_vendor.id
    ).all()
    return products

@router.post("/menu", response_model=schemas.ProductResponse, status_code=status.HTTP_201_CREATED)
def add_menu_item(
    product_data: schemas.ProductCreate,
    current_vendor: models.User = Depends(auth.get_current_vendor),
    db: Session = Depends(get_db)
):
    new_product = models.Product(
        vendor_profile_id=current_vendor.id,
        name=product_data.name,
        description=product_data.description,
        price=product_data.price,
        category=product_data.category,
        is_available=True
    )
    db.add(new_product)
    db.commit()
    db.refresh(new_product)
    return new_product


# --- Transactions (POS Sales) ---

@router.post("/transactions", response_model=schemas.TransactionResponse, status_code=status.HTTP_201_CREATED)
def record_transaction(
    tx_data: schemas.TransactionCreate,
    current_vendor: models.User = Depends(auth.get_current_vendor),
    db: Session = Depends(get_db)
):
    if not tx_data.items:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Transaction must contain at least one item."
        )

    # 1. Fetch item prices and calculate total amount
    total_amount = 0.0
    items_to_create = []

    for item in tx_data.items:
        product = db.query(models.Product).filter(
            models.Product.id == item.product_id,
            models.Product.vendor_profile_id == current_vendor.id
        ).first()

        if not product:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Product with ID {item.product_id} not found on your menu."
            )
        
        unit_price = float(product.price)
        total_amount += unit_price * item.quantity

        items_to_create.append(
            models.TransactionItem(
                product_id=product.id,
                quantity=item.quantity,
                unit_price=unit_price
            )
        )

    # 2. Create the Transaction record
    new_tx = models.Transaction(
        vendor_profile_id=current_vendor.id,
        total_amount=total_amount,
        payment_method=tx_data.payment_method
    )
    db.add(new_tx)
    db.flush()  # gets new_tx.id

    # 3. Associate items and insert
    for tx_item in items_to_create:
        tx_item.transaction_id = new_tx.id
        db.add(tx_item)

    db.commit()
    db.refresh(new_tx)

    return schemas.TransactionResponse(
        transaction_id=new_tx.id,
        total_amount=float(new_tx.total_amount),
        payment_method=new_tx.payment_method,
        created_at=new_tx.created_at
    )


# --- Expenses ---

@router.post("/expenses", response_model=schemas.ExpenseResponse, status_code=status.HTTP_201_CREATED)
def log_expense(
    exp_data: schemas.ExpenseCreate,
    current_vendor: models.User = Depends(auth.get_current_vendor),
    db: Session = Depends(get_db)
):
    new_expense = models.Expense(
        vendor_profile_id=current_vendor.id,
        amount=exp_data.amount,
        description=exp_data.description,
        category=exp_data.category
    )
    db.add(new_expense)
    db.commit()
    db.refresh(new_expense)
    return new_expense


# --- Location and Status updates ---

@router.post("/location")
def update_location(
    loc_data: schemas.LocationUpdate,
    current_vendor: models.User = Depends(auth.get_current_vendor),
    db: Session = Depends(get_db),
    redis: Redis = Depends(get_redis)
):
    # 1. Update relational profile in PostgreSQL
    profile = db.query(models.VendorProfile).filter(
        models.VendorProfile.id == current_vendor.id
    ).first()
    
    if not profile:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Vendor profile not found."
        )

    profile.last_latitude = loc_data.latitude
    profile.last_longitude = loc_data.longitude
    profile.status = loc_data.status
    profile.last_location_updated_at = datetime.utcnow()
    profile.is_active = (loc_data.status != "closed")
    db.commit()

    # 2. Synchronize active state to Redis Geostore
    redis_key = "vendor:locations"
    vendor_id_str = str(current_vendor.id)

    if loc_data.status != "closed":
        # Redis GEOADD expects (longitude, latitude, member)
        redis.geoadd(redis_key, (loc_data.longitude, loc_data.latitude, vendor_id_str))
        
        # Cache status details
        redis.hset(
            f"vendor:status:{vendor_id_str}",
            mapping={
                "status": loc_data.status,
                "business_name": profile.business_name,
                "rating": str(profile.rating),
                "updated_at": datetime.utcnow().isoformat()
            }
        )
    else:
        # If vendor goes offline, clean up Redis
        redis.zrem(redis_key, vendor_id_str)
        redis.delete(f"vendor:status:{vendor_id_str}")

    return {
        "message": "Location and status updated successfully",
        "timestamp": datetime.utcnow()
    }
