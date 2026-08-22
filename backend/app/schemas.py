from pydantic import BaseModel, Field
from uuid import UUID
from datetime import datetime
from typing import List, Optional

# --- Authentication Schemas ---

class UserCreate(BaseModel):
    phone: str = Field(..., min_length=10, max_length=15, description="Vendor or Customer phone number")
    password: str = Field(..., min_length=6, description="Minimum 6 characters password")
    role: str = Field(..., description="'vendor' or 'customer'")
    business_name: Optional[str] = Field(None, description="Required if role is 'vendor'")

class UserResponse(BaseModel):
    id: UUID
    phone: str
    role: str
    created_at: datetime

    class Config:
        from_attributes = True

class UserLogin(BaseModel):
    phone: str
    password: str

class TokenUser(BaseModel):
    id: UUID
    phone: str
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

class NearbyVendorResponse(BaseModel):
    vendor_id: UUID
    business_name: str
    status: str
    latitude: float
    longitude: float
    rating: float
    distance_km: float

class CustomerSearchResponse(BaseModel):
    vendor_id: UUID
    business_name: str
    status: str
    latitude: float
    longitude: float
    rating: float
