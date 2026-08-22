import uuid
from sqlalchemy import Column, String, Boolean, Numeric, Float, DateTime, ForeignKey, Integer, Text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.database import Base

class User(Base):
    __tablename__ = "users"
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    email = Column(String(255), unique=True, nullable=False, index=True)
    role = Column(String(10), nullable=False)  # 'vendor' or 'customer'
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    vendor_profile = relationship("VendorProfile", uselist=False, back_populates="user", cascade="all, delete-orphan")
    search_logs = relationship("SearchLog", back_populates="user")

class VendorProfile(Base):
    __tablename__ = "vendor_profiles"
    id = Column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), primary_key=True)
    business_name = Column(String(100), nullable=False)
    description = Column(Text, nullable=True)
    is_active = Column(Boolean, default=False, nullable=False)
    status = Column(String(20), default="closed", nullable=False)  # 'open', 'closed', 'moving'
    last_latitude = Column(Float, nullable=True)
    last_longitude = Column(Float, nullable=True)
    last_location_updated_at = Column(DateTime(timezone=True), nullable=True)
    rating = Column(Numeric(3, 2), default=5.00, nullable=False)

    user = relationship("User", back_populates="vendor_profile")
    products = relationship("Product", back_populates="vendor_profile", cascade="all, delete-orphan")
    transactions = relationship("Transaction", back_populates="vendor_profile")
    expenses = relationship("Expense", back_populates="vendor_profile", cascade="all, delete-orphan")

class Product(Base):
    __tablename__ = "products"
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    vendor_profile_id = Column(UUID(as_uuid=True), ForeignKey("vendor_profiles.id", ondelete="CASCADE"), nullable=False, index=True)
    name = Column(String(100), nullable=False)
    description = Column(Text, nullable=True)
    price = Column(Numeric(10, 2), nullable=False)
    category = Column(String(50), default="snacks", nullable=False)
    is_available = Column(Boolean, default=True, nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    vendor_profile = relationship("VendorProfile", back_populates="products")
    transaction_items = relationship("TransactionItem", back_populates="product")

class Transaction(Base):
    __tablename__ = "transactions"
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    vendor_profile_id = Column(UUID(as_uuid=True), ForeignKey("vendor_profiles.id", ondelete="SET NULL"), nullable=True, index=True)
    total_amount = Column(Numeric(10, 2), nullable=False)
    payment_method = Column(String(20), default="cash", nullable=False)  # 'cash', 'upi'
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False, index=True)

    vendor_profile = relationship("VendorProfile", back_populates="transactions")
    items = relationship("TransactionItem", back_populates="transaction", cascade="all, delete-orphan")

class TransactionItem(Base):
    __tablename__ = "transaction_items"
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    transaction_id = Column(UUID(as_uuid=True), ForeignKey("transactions.id", ondelete="CASCADE"), nullable=False, index=True)
    product_id = Column(UUID(as_uuid=True), ForeignKey("products.id", ondelete="RESTRICT"), nullable=False, index=True)
    quantity = Column(Integer, nullable=False)
    unit_price = Column(Numeric(10, 2), nullable=False)

    transaction = relationship("Transaction", back_populates="items")
    product = relationship("Product", back_populates="transaction_items")

class Expense(Base):
    __tablename__ = "expenses"
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    vendor_profile_id = Column(UUID(as_uuid=True), ForeignKey("vendor_profiles.id", ondelete="CASCADE"), nullable=False, index=True)
    amount = Column(Numeric(10, 2), nullable=False)
    description = Column(String(255), nullable=False)
    category = Column(String(50), nullable=False)  # 'raw_materials', 'gas', 'rent', 'other'
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False, index=True)

    vendor_profile = relationship("VendorProfile", back_populates="expenses")

class SearchLog(Base):
    __tablename__ = "search_logs"
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    query = Column(String(100), nullable=False, index=True)
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="SET NULL"), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False, index=True)

    user = relationship("User", back_populates="search_logs")
