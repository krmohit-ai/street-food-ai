import uuid
import math
import os
# pyrefly: ignore [missing-import]
import joblib
import pandas as pd
import google.generativeai as genai
from datetime import datetime, timedelta
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session, joinedload
from sqlalchemy import func
from redis import Redis
from app.database import get_db, get_redis
from app import models, schemas, auth
from app.config import settings

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

@router.put("/profile", response_model=schemas.VendorProfileResponse)
def update_profile(
    profile_data: schemas.VendorProfileUpdate,
    current_vendor: models.User = Depends(auth.get_current_vendor),
    db: Session = Depends(get_db)
):
    profile = db.query(models.VendorProfile).filter(
        models.VendorProfile.id == current_vendor.id
    ).first()

    if not profile:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Vendor profile not found."
        )

    for field, value in profile_data.model_dump(exclude_unset=True).items():
        setattr(profile, field, value)

    db.commit()
    db.refresh(profile)
    return profile

@router.get("/profile", response_model=schemas.VendorProfileResponse)
def get_profile(
    current_vendor: models.User = Depends(auth.get_current_vendor),
    db: Session = Depends(get_db)
):
    profile = db.query(models.VendorProfile).filter(
        models.VendorProfile.id == current_vendor.id
    ).first()
    if not profile:
        raise HTTPException(status_code=404, detail="Profile not found")
    return profile

@router.get("/reviews", response_model=list[schemas.ReviewResponse])
def get_vendor_reviews(
    current_vendor: models.User = Depends(auth.get_current_vendor),
    db: Session = Depends(get_db)
):
    reviews = db.query(models.Review).options(joinedload(models.Review.customer)).filter(
        models.Review.vendor_profile_id == current_vendor.id
    ).order_by(models.Review.created_at.desc()).all()

    return [
        schemas.ReviewResponse(
            id=r.id,
            customer_name=r.customer.email.split("@")[0] if r.customer else "Anonymous",
            rating=r.rating,
            comment=r.comment,
            created_at=r.created_at
        ) for r in reviews
    ]

@router.get("/transactions", response_model=list[schemas.TransactionResponse])
def get_transaction_history(current_vendor: models.User = Depends(auth.get_current_vendor), db: Session = Depends(get_db)):
    return db.query(models.Transaction).filter(models.Transaction.vendor_profile_id == current_vendor.id).order_by(models.Transaction.created_at.desc()).all()

@router.get("/expenses", response_model=list[schemas.ExpenseResponse])
def get_expense_history(current_vendor: models.User = Depends(auth.get_current_vendor), db: Session = Depends(get_db)):
    return db.query(models.Expense).filter(models.Expense.vendor_profile_id == current_vendor.id).order_by(models.Expense.created_at.desc()).all()


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
                "business_name": profile.business_name or "New Vendor",
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


# --- Haversine Distance Helper ---
def calculate_distance(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    R = 6371.0  # Earth's radius in km
    dlat = math.radians(lat2 - lat1)
    dlon = math.radians(lon2 - lon1)
    a = math.sin(dlat / 2)**2 + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dlon / 2)**2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return R * c


# --- Spatial Grid Helpers ---
MIN_LAT, MAX_LAT = 12.9250, 12.9450
MIN_LON, MAX_LON = 77.6140, 77.6350
LAT_STEP = 0.002
LON_STEP = 0.0021

def get_grid_coordinates(lat: float, lon: float):
    grid_y = int((lat - MIN_LAT) / LAT_STEP)
    grid_x = int((lon - MIN_LON) / LON_STEP)
    grid_y = max(0, min(9, grid_y))
    grid_x = max(0, min(9, grid_x))
    return grid_y, grid_x

def get_grid_center(grid_y: int, grid_x: int):
    lat = MIN_LAT + (grid_y * LAT_STEP) + (LAT_STEP / 2)
    lon = MIN_LON + (grid_x * LON_STEP) + (LON_STEP / 2)
    return round(lat, 6), round(lon, 6)


# --- Analytics Endpoint ---
@router.get("/analytics")
def get_analytics(
    range: str = "weekly",
    current_vendor: models.User = Depends(auth.get_current_vendor),
    db: Session = Depends(get_db)
):
    # 1. Revenue based on range
    today = datetime.utcnow().date()
    start_date = today
    if range == "daily":
        start_date = today
    elif range == "weekly":
        start_date = today - timedelta(days=7)
    elif range == "monthly":
        start_date = today - timedelta(days=30)
    elif range == "yearly":
        start_date = today - timedelta(days=365)
    
    weekly_data = db.query(
        func.date(models.Transaction.created_at).label("date"),
        func.sum(models.Transaction.total_amount).label("revenue")
    ).filter(
        models.Transaction.vendor_profile_id == current_vendor.id,
        models.Transaction.created_at >= start_date
    ).group_by(
        func.date(models.Transaction.created_at)
    ).order_by(
        "date"
    ).all()
    
    weekly_revenue = [{"date": str(row.date), "revenue": float(row.revenue or 0)} for row in weekly_data]
    
    # 2. Hourly Sales (sales volume distribution in the range)
    hourly_data = db.query(
        func.extract("hour", models.Transaction.created_at).label("hour"),
        func.count(models.Transaction.id).label("count"),
        func.sum(models.Transaction.total_amount).label("revenue")
    ).filter(
        models.Transaction.vendor_profile_id == current_vendor.id,
        models.Transaction.created_at >= start_date
    ).group_by(
        func.extract("hour", models.Transaction.created_at)
    ).all()
    
    hourly_sales = [{"hour": int(row.hour), "count": int(row.count), "revenue": float(row.revenue or 0)} for row in hourly_data]
    
    # 3. Category Distribution
    category_data = db.query(
        models.Product.category.label("category"),
        func.sum(models.TransactionItem.quantity * models.TransactionItem.unit_price).label("sales")
    ).select_from(models.TransactionItem).join(
        models.Transaction, models.TransactionItem.transaction_id == models.Transaction.id
    ).join(
        models.Product, models.TransactionItem.product_id == models.Product.id
    ).filter(
        models.Transaction.vendor_profile_id == current_vendor.id,
        models.Transaction.created_at >= start_date
    ).group_by(
        models.Product.category
    ).all()
    
    category_distribution = [{"category": row.category, "sales": float(row.sales or 0)} for row in category_data]
    
    # 4. Profit Margin (Revenue vs Expenses in range)
    total_rev = db.query(func.sum(models.Transaction.total_amount)).filter(
        models.Transaction.vendor_profile_id == current_vendor.id,
        models.Transaction.created_at >= start_date
    ).scalar() or 0
    
    total_exp = db.query(func.sum(models.Expense.amount)).filter(
        models.Expense.vendor_profile_id == current_vendor.id,
        models.Expense.created_at >= start_date
    ).scalar() or 0
    
    profit_margin = {
        "total_revenue": float(total_rev),
        "total_expenses": float(total_exp),
        "net_profit": float(total_rev - total_exp)
    }
    
    return {
        "weekly_revenue": weekly_revenue,
        "hourly_sales": hourly_sales,
        "category_distribution": category_distribution,
        "profit_margin": profit_margin
    }


# --- Recommendations (AI Demand Forecasting + Gemini API) ---
MODEL_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../../ml/models/xgboost_demand.joblib"))

@router.get("/recommendations")
def get_recommendations(
    current_vendor: models.User = Depends(auth.get_current_vendor),
    db: Session = Depends(get_db)
):
    profile = db.query(models.VendorProfile).filter(
        models.VendorProfile.id == current_vendor.id
    ).first()
    
    if not profile or profile.last_latitude is None or profile.last_longitude is None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Vendor profile details or current GPS location missing. Update location first."
        )

    # 1. Load forecasting model
    if not os.path.exists(MODEL_PATH):
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Forecasting model is not trained yet."
        )
    model = joblib.load(MODEL_PATH)

    # 2. Get temporal parameters for next hour
    now = datetime.utcnow()
    next_hour = (now.hour + 1) % 24
    day_of_week = now.weekday()

    # 3. Get currently active supply in all cells
    active_vendors = db.query(models.VendorProfile).filter(
        models.VendorProfile.is_active == True
    ).all()

    supply_counts = {}
    for v in active_vendors:
        # Skip current vendor to see supply gap correctly
        if v.id == current_vendor.id:
            continue
        if v.last_latitude is not None and v.last_longitude is not None:
            gy, gx = get_grid_coordinates(v.last_latitude, v.last_longitude)
            supply_counts[(gy, gx)] = supply_counts.get((gy, gx), 0) + 1

    # 4. Forecast demand and rank all cells
    recommendations_list = []
    for gy in range(10):
        for gx in range(10):
            # Predict demand via model
            input_df = pd.DataFrame([{
                'grid_y': gy,
                'grid_x': gx,
                'hour': next_hour,
                'day_of_week': day_of_week
            }])
            forecasted_demand = float(max(0.0, model.predict(input_df)[0]))
            
            # Competitors in this cell
            supply = supply_counts.get((gy, gx), 0)
            
            # Score logic
            score = forecasted_demand / (supply + 1)
            
            lat_center, lon_center = get_grid_center(gy, gx)
            distance = calculate_distance(profile.last_latitude, profile.last_longitude, lat_center, lon_center)

            recommendations_list.append({
                "grid_y": gy,
                "grid_x": gx,
                "latitude": lat_center,
                "longitude": lon_center,
                "forecasted_demand": forecasted_demand,
                "active_competitors": supply,
                "score": score,
                "distance_km": round(distance, 2)
            })

    # Sort descending by gap score
    recommendations_list.sort(key=lambda x: x["score"], reverse=True)
    top_3 = recommendations_list[:3]

    # 5. Gemini API advice generation
    if settings.GEMINI_API_KEY:
        genai.configure(api_key=settings.GEMINI_API_KEY)
        
    categories = [p.category for p in profile.products]
    categories_str = ", ".join(set(categories)) if categories else "snacks"
    
    prompt = f"""
    You are an AI street-food business advisor in India. A vendor with the business '{profile.business_name}' selling '{categories_str}' is asking for moving and inventory advice.
    Here is our demand forecasting and market density data for the upcoming hour:
    1. Hotspot 1 (Lat: {top_3[0]['latitude']}, Lon: {top_3[0]['longitude']}): Estimated demand is Rs {top_3[0]['forecasted_demand']:.0f}, with {top_3[0]['active_competitors']} competitors nearby. Distance is {top_3[0]['distance_km']:.2f} km.
    2. Hotspot 2 (Lat: {top_3[1]['latitude']}, Lon: {top_3[1]['longitude']}): Estimated demand is Rs {top_3[1]['forecasted_demand']:.0f}, with {top_3[1]['active_competitors']} competitors nearby. Distance is {top_3[1]['distance_km']:.2f} km.
    
    Provide 2-3 bulleted, concise, actionable business recommendations (e.g. where to shift, what inventory prep to prioritize) for the vendor. Limit output to under 100 words. Keep tone encouraging and helpful.
    """

    try:
        if settings.GEMINI_API_KEY:
            gemini_model = genai.GenerativeModel('gemini-1.5-flash')
            response = gemini_model.generate_content(prompt)
            advice = response.text.strip()
        else:
            raise ValueError("No API key")
    except Exception:
        # Fallback advice if Gemini API fails or key is missing
        advice = (
            f"• High demand of Rs {top_3[0]['forecasted_demand']:.0f} detected at Hotspot 1 (Lat: {top_3[0]['latitude']}, Lon: {top_3[0]['longitude']}), just {top_3[0]['distance_km']:.2f} km away. It has zero competitive density.\n"
            f"• Prioritize stocking key ingredients for {categories_str} as demand is projected to spike within the hour."
        )

    # 6. Fetch recent demand signals (Search logs with 10-min delay for realism)
    ten_mins_ago = datetime.utcnow() - timedelta(minutes=10)
    two_hours_ago = datetime.utcnow() - timedelta(hours=2)

    recent_searches = db.query(
        models.SearchLog.query.label("item_name"),
        models.SearchLog.latitude,
        models.SearchLog.longitude
    ).filter(
        models.SearchLog.created_at >= two_hours_ago,
        models.SearchLog.created_at <= ten_mins_ago
    ).all()

    demand_hotspots = []
    for s in recent_searches:
        demand_hotspots.append({
            "item_name": s.item_name.replace("DEMAND:", "").capitalize(),
            "latitude": s.latitude,
            "longitude": s.longitude,
            "count": 1 # For demo, each log is 1 signal
        })

    return {
        "vendor_id": current_vendor.id,
        "recommendations": top_3,
        "demand_hotspots": demand_hotspots,
        "ai_advice": advice,
        "timestamp": datetime.utcnow()
    }