import uuid
from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from redis import Redis
from app.database import get_db, get_redis
from app import models, schemas, auth

router = APIRouter(prefix="/customer", tags=["Customer Operations"])

@router.get("/vendors", response_model=list[schemas.NearbyVendorResponse])
def get_nearby_vendors(
    latitude: float,
    longitude: float,
    radius: float = 2.0,
    db: Session = Depends(get_db),
    redis: Redis = Depends(get_redis)
):
    redis_key = "vendor:locations"
    
    # 1. Query Redis for members within radius
    # georadius syntax: georadius(name, longitude, latitude, radius, unit="km", withdist=True, withcoord=True)
    raw_results = redis.georadius(
        redis_key, 
        longitude, 
        latitude, 
        radius, 
        unit="km", 
        withdist=True, 
        withcoord=True
    )
    
    nearby_vendors = []
    
    for item in raw_results:
        member_id = item[0]
        dist = item[1]
        coords = item[2]
        
        vendor_uuid = uuid.UUID(member_id)
        
        # 2. Get profile from PostgreSQL
        profile = db.query(models.VendorProfile).filter(
            models.VendorProfile.id == vendor_uuid
        ).first()
        
        if not profile:
            continue
            
        nearby_vendors.append(
            schemas.NearbyVendorResponse(
                vendor_id=vendor_uuid,
                business_name=profile.business_name,
                status=profile.status,
                latitude=coords[1],
                longitude=coords[0],
                rating=float(profile.rating),
                distance_km=float(dist)
            )
        )
        
    nearby_vendors.sort(key=lambda x: x.distance_km)
    return nearby_vendors


@router.get("/search", response_model=list[schemas.CustomerSearchResponse])
def search_food(
    query: str,
    latitude: float,
    longitude: float,
    db: Session = Depends(get_db),
    redis: Redis = Depends(get_redis)
):
    # 1. Log the search telemetry (our anonymous demand signals!)
    new_log = models.SearchLog(
        query=query.strip().lower(),
        latitude=latitude,
        longitude=longitude
    )
    db.add(new_log)
    db.commit()

    # 2. Find all active vendors in the area (2km radius)
    redis_key = "vendor:locations"
    raw_results = redis.georadius(
        redis_key, 
        longitude, 
        latitude, 
        2.0,  # 2km search radius for demo
        unit="km", 
        withcoord=True
    )

    vendor_ids = [uuid.UUID(res[0]) for res in raw_results]
    if not vendor_ids:
        return []

    # 3. Filter vendors who have products matching the query (case insensitive)
    matching_vendors = db.query(models.VendorProfile).join(models.Product).filter(
        models.VendorProfile.id.in_(vendor_ids),
        models.Product.is_available == True,
        (models.Product.name.ilike(f"%{query}%") | models.Product.category.ilike(f"%{query}%"))
    ).distinct().all()

    # Build response list
    results = []
    vendor_coord_map = {uuid.UUID(res[0]): res[1] for res in raw_results}

    for vendor in matching_vendors:
        coords = vendor_coord_map.get(vendor.id)
        results.append(
            schemas.CustomerSearchResponse(
                vendor_id=vendor.id,
                business_name=vendor.business_name,
                status=vendor.status,
                latitude=coords[1] if coords else (vendor.last_latitude or 0.0),
                longitude=coords[0] if coords else (vendor.last_longitude or 0.0),
                rating=float(vendor.rating)
            )
        )
    return results

@router.get("/vendor/{vendor_id}/menu", response_model=list[schemas.ProductResponse])
def get_vendor_menu(vendor_id: uuid.UUID, db: Session = Depends(get_db)):
    products = db.query(models.Product).filter(
        models.Product.vendor_profile_id == vendor_id,
        models.Product.is_available == True
    ).all()
    return products

@router.post("/vendor/{vendor_id}/reviews", response_model=schemas.ReviewResponse)
def post_review(
    vendor_id: uuid.UUID,
    review: schemas.ReviewCreate,
    current_user: models.User = Depends(auth.get_current_user),
    db: Session = Depends(get_db)
):
    # 1. Verify vendor exists
    vendor = db.query(models.VendorProfile).filter(models.VendorProfile.id == vendor_id).first()
    if not vendor:
        raise HTTPException(status_code=404, detail="Vendor not found")

    # 2. Save review
    new_review = models.Review(
        vendor_profile_id=vendor_id,
        customer_id=current_user.id,
        rating=review.rating,
        comment=review.comment
    )
    db.add(new_review)

    # 3. Update vendor rating (average)
    avg_rating = db.query(func.avg(models.Review.rating)).filter(
        models.Review.vendor_profile_id == vendor_id
    ).scalar() or review.rating

    vendor.rating = float(avg_rating)

    db.commit()
    db.refresh(new_review)

    return schemas.ReviewResponse(
        id=new_review.id,
        customer_name=current_user.email.split("@")[0],
        rating=new_review.rating,
        comment=new_review.comment,
        created_at=new_review.created_at
    )

@router.post("/demand")
def create_demand(
    demand: schemas.DemandCreate,
    db: Session = Depends(get_db)
):
    new_log = models.SearchLog(
        query=f"DEMAND:{demand.item_name}",
        latitude=demand.latitude,
        longitude=demand.longitude
    )
    db.add(new_log)
    db.commit()
    return {"message": "Demand registered. Vendors will see this soon!"}
