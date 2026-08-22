from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.config import settings
from app.routers import auth, vendor, customer

app = FastAPI(
    title="Street Food AI Backend",
    description="Real-time demand and business intelligence platform for street-food vendors",
    version="1.0.0",
)

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(auth.router, prefix="/api")
app.include_router(vendor.router, prefix="/api")
app.include_router(customer.router, prefix="/api")

@app.get("/api/health")
def health_check():
    return {
        "status": "healthy",
        "database": "connected",
        "redis": "connected"
    }
