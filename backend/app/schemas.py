from pydantic import BaseModel, Field
from uuid import UUID
from datetime import datetime
from typing import List, Optional

# --- Authentication Schemas ---

class GoogleLoginRequest(BaseModel):
    id_token: str = Field(..., description="Google ID Token JWT")
    role: Optional[str] = Field(None, description="'vendor' or 'customer' (required only for registration)")
    business_name: Optional[str] = Field(None, description="Required if role is 'vendor' and registering")

class UserResponse(BaseModel):
    id: UUID
    email: str
    role: str
    created_at: datetime

    class Config:
        from_attributes = True

class TokenUser(BaseModel):
    id: UUID
    email: str
    role: str

    class Config:
        from_attributes = True

class Token(BaseModel):
    access_token: str
    token_type: str
    user: TokenUser

# --- Product/Menu Schemas ---

class ProductCreate(BaseModel):
    name: str = Field(..., min_length=2, max_length=100)
    description: Optional[str] = None
    price: float = Field(..., gt=0)
    category: str = Field("snacks", description="snacks, beverages, etc.")

class ProductResponse(BaseModel):
    id: UUID
    name: str
    description: Optional[str]
    price: float
    category: str
    is_available: bool

    class Config:
        from_attributes = True

# --- Transaction / POS Schemas ---

class TransactionItemCreate(BaseModel):
    product_id: UUID
    quantity: int = Field(..., gt=0)

class TransactionCreate(BaseModel):
    payment_method: str = Field("cash", description="'cash' or 'upi'")
    items: List[TransactionItemCreate]

class TransactionResponse(BaseModel):
    transaction_id: UUID
    total_amount: float
    payment_method: str
    created_at: datetime

    class Config:
        from_attributes = True

# --- Expense Schemas ---

class ExpenseCreate(BaseModel):
    amount: float = Field(..., gt=0)
    description: str = Field(..., min_length=2, max_length=255)
    category: str = Field(..., description="raw_materials, gas, rent, other")

class ExpenseResponse(BaseModel):
    id: UUID
    amount: float
    description: str
    category: str
    created_at: datetime

    class Config:
        from_attributes = True

# --- Location & Maps Schemas ---

class LocationUpdate(BaseModel):
    latitude: float = Field(..., ge=-90, le=90)
    longitude: float = Field(..., ge=-180, le=180)
    status: str = Field("open", description="open, closed, moving")

class VendorProfileUpdate(BaseModel):
    business_name: Optional[str] = None
    description: Optional[str] = None
    what_he_sells: Optional[str] = None
    regularity: Optional[str] = "regular"
    is_movable: Optional[bool] = True
    typical_latitude: Optional[float] = None
    typical_longitude: Optional[float] = None
    approx_sales_per_day: Optional[float] = None

class VendorProfileResponse(BaseModel):
    id: UUID
    business_name: str
    description: Optional[str]
    rating: float
    what_he_sells: Optional[str]
    regularity: Optional[str]
    is_movable: bool
    typical_latitude: Optional[float]
    typical_longitude: Optional[float]
    approx_sales_per_day: Optional[float]

    class Config:
        from_attributes = True

class NearbyVendorResponse(BaseModel):
    vendor_id: UUID
    business_name: str
    status: str
    latitude: float
    longitude: float
    rating: float
    distance_km: float

class ReviewCreate(BaseModel):
    rating: int = Field(..., ge=1, le=5)
    comment: Optional[str] = Field(None, max_length=500)

class ReviewResponse(BaseModel):
    id: UUID
    customer_name: str
    rating: int
    comment: Optional[str]
    created_at: datetime

    class Config:
        from_attributes = True

class DemandCreate(BaseModel):
    item_name: str = Field(..., min_length=2, max_length=50)
    latitude: float
    longitude: float

class CustomerSearchResponse(BaseModel):
    vendor_id: UUID
    business_name: str
    status: str
    latitude: float
    longitude: float
    rating: float
