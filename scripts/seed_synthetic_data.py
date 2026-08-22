import sys
import os
import random
import uuid
from datetime import datetime, timedelta

# Add backend directory to Python path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '../backend')))

from app.database import SessionLocal, Base, engine
from app import models, auth

def seed_data():
    db = SessionLocal()
    print("Clearing existing data...")
    # Delete in order of foreign key dependencies
    db.query(models.SearchLog).delete()
    db.query(models.TransactionItem).delete()
    db.query(models.Transaction).delete()
    db.query(models.Expense).delete()
    db.query(models.Product).delete()
    db.query(models.VendorProfile).delete()
    db.query(models.User).delete()
    db.commit()

    print("Seeding users and profiles...")
    
    # 1. Create Momo Vendor
    momo_user = models.User(
        id=uuid.uuid4(),
        phone="9876543210",
        password_hash=auth.get_password_hash("password123"),
        role="vendor"
    )
    db.add(momo_user)
    db.flush()
    
    momo_profile = models.VendorProfile(
        id=momo_user.id,
        business_name="Koramangala Momo Spot",
        description="Juicy steam and fried veg & chicken momos",
        is_active=True,
        status="open",
        last_latitude=12.9352,
        last_longitude=77.6245,
        last_location_updated_at=datetime.utcnow(),
        rating=4.8
    )
    db.add(momo_profile)

    # 2. Create Chai Vendor
    chai_user = models.User(
        id=uuid.uuid4(),
        phone="9876543211",
        password_hash=auth.get_password_hash("password123"),
        role="vendor"
    )
    db.add(chai_user)
    db.flush()
    
    chai_profile = models.VendorProfile(
        id=chai_user.id,
        business_name="Chai Point Express",
        description="Hot ginger masala tea and crispy samosas",
        is_active=True,
        status="open",
        last_latitude=12.9312,
        last_longitude=77.6210,
        last_location_updated_at=datetime.utcnow(),
        rating=4.6
    )
    db.add(chai_profile)
    db.commit()

    print("Seeding products...")
    # 3. Create Products
    momo_steam = models.Product(
        id=uuid.uuid4(),
        vendor_profile_id=momo_profile.id,
        name="Steam Momos",
        description="Hot veg steam momos (8 pcs)",
        price=80.00,
        category="snacks",
        is_available=True
    )
    momo_fried = models.Product(
        id=uuid.uuid4(),
        vendor_profile_id=momo_profile.id,
        name="Fried Momos",
        description="Crispy fried veg momos (8 pcs)",
        price=100.00,
        category="snacks",
        is_available=True
    )
    chai_masala = models.Product(
        id=uuid.uuid4(),
        vendor_profile_id=chai_profile.id,
        name="Masala Chai",
        description="Indian ginger cardamom tea",
        price=15.00,
        category="beverages",
        is_available=True
    )
    chai_samosa = models.Product(
        id=uuid.uuid4(),
        vendor_profile_id=chai_profile.id,
        name="Samosa",
        description="Crispy potato stuffed samosa (1 pc)",
        price=20.00,
        category="snacks",
        is_available=True
    )
    db.add_all([momo_steam, momo_fried, chai_masala, chai_samosa])
    db.commit()

    print("Generating 6 months of sales transactions...")
    # We will generate sales going back 180 days
    start_date = datetime.utcnow() - timedelta(days=180)
    current_time = datetime.utcnow()

    transactions = []
    transaction_items = []
    expenses = []
    search_logs = []

    # Bounded locations:
    # Momo Spot coordinates centered around College Road: Lat: 12.9352, Lon: 77.6245
    # Chai Spot coordinates centered around Main Market: Lat: 12.9312, Lon: 77.6210

    # Let's run a loop day by day
    for day_offset in range(180):
        day = start_date + timedelta(days=day_offset)
        day_of_week = day.weekday() # 0 = Monday, 6 = Sunday
        
        # --- Generate Sales for Momo Spot ---
        # Momos have high demand in the evenings (5 PM to 10 PM), especially on Fri (4) and Sat (5)
        momo_base_sales = 15 if day_of_week in [4, 5] else 8
        for hour in range(17, 22):
            # Sales probability distribution: peak around 7-8 PM
            hour_factor = 1.5 if hour in [19, 20] else 1.0
            sales_count = int(random.randint(momo_base_sales - 3, momo_base_sales + 3) * hour_factor)
            
            for _ in range(sales_count):
                tx_id = uuid.uuid4()
                # Steam momos vs Fried momos split
                steam_qty = random.randint(1, 2)
                fried_qty = random.choices([0, 1, 2], weights=[0.4, 0.4, 0.2])[0]
                
                total = (steam_qty * 80.00) + (fried_qty * 100.00)
                if total == 0:
                    continue
                
                tx_time = day.replace(hour=hour, minute=random.randint(0, 59), second=random.randint(0, 59))
                transactions.append(
                    models.Transaction(
                        id=tx_id,
                        vendor_profile_id=momo_profile.id,
                        total_amount=total,
                        payment_method=random.choices(["cash", "upi"], weights=[0.3, 0.7])[0],
                        created_at=tx_time
                    )
                )
                
                transaction_items.append(
                    models.TransactionItem(
                        id=uuid.uuid4(),
                        transaction_id=tx_id,
                        product_id=momo_steam.id,
                        quantity=steam_qty,
                        unit_price=80.00
                    )
                )
                if fried_qty > 0:
                    transaction_items.append(
                        models.TransactionItem(
                            id=uuid.uuid4(),
                            transaction_id=tx_id,
                            product_id=momo_fried.id,
                            quantity=fried_qty,
                            unit_price=100.00
                        )
                    )

        # --- Generate Sales for Chai Spot ---
        # Chai has high demand in mornings (8 AM to 11 AM) and afternoons (3 PM to 6 PM) daily
        for hour in [8, 9, 10, 15, 16, 17]:
            chai_base_sales = 20
            # peak demand factor during tea breaks (9 AM and 4 PM)
            hour_factor = 1.8 if hour in [9, 16] else 1.0
            sales_count = int(random.randint(chai_base_sales - 5, chai_base_sales + 5) * hour_factor)
            
            for _ in range(sales_count):
                tx_id = uuid.uuid4()
                chai_qty = random.randint(1, 4)
                samosa_qty = random.choices([0, 1, 2], weights=[0.5, 0.4, 0.1])[0]
                
                total = (chai_qty * 15.00) + (samosa_qty * 20.00)
                tx_time = day.replace(hour=hour, minute=random.randint(0, 59), second=random.randint(0, 59))
                
                transactions.append(
                    models.Transaction(
                        id=tx_id,
                        vendor_profile_id=chai_profile.id,
                        total_amount=total,
                        payment_method=random.choices(["cash", "upi"], weights=[0.6, 0.4])[0],
                        created_at=tx_time
                    )
                )
                
                transaction_items.append(
                    models.TransactionItem(
                        id=uuid.uuid4(),
                        transaction_id=tx_id,
                        product_id=chai_masala.id,
                        quantity=chai_qty,
                        unit_price=15.00
                    )
                )
                if samosa_qty > 0:
                    transaction_items.append(
                        models.TransactionItem(
                            id=uuid.uuid4(),
                            transaction_id=tx_id,
                            product_id=chai_samosa.id,
                            quantity=samosa_qty,
                            unit_price=20.00
                        )
                    )

        # --- Log Weekly Expenses ---
        # Let's log expenses every Sunday (6)
        if day_of_week == 6:
            # Momo Spot raw materials + gas
            expenses.append(
                models.Expense(
                    id=uuid.uuid4(),
                    vendor_profile_id=momo_profile.id,
                    amount=float(random.randint(2500, 3500)),
                    description="Flour, vegetables, chicken, spices, and gas cylinder refill",
                    category="raw_materials",
                    created_at=day.replace(hour=10, minute=0)
                )
            )
            # Chai Spot raw materials
            expenses.append(
                models.Expense(
                    id=uuid.uuid4(),
                    vendor_profile_id=chai_profile.id,
                    amount=float(random.randint(1500, 2500)),
                    description="Tea leaves, milk, ginger, cardamom, potatoes, oil",
                    category="raw_materials",
                    created_at=day.replace(hour=11, minute=0)
                )
            )

        # --- Generate Anonymous Customer Search Telemetry (Demand Signals) ---
        # Generate some search logs that correlate with demand
        # We simulate searches starting 7 days ago to keep it fresh and dense for real-time demo
        if day > (current_time - timedelta(days=7)):
            # Searches for momos near college road (high) and market (low)
            # Peak search interest is Friday evening
            momo_searches = 30 if day_of_week in [4, 5] else 10
            for _ in range(momo_searches):
                # Cluster coordinates around College Road grid (Lat: 12.9360 + noise, Lon: 77.6250 + noise)
                lat = 12.9360 + random.uniform(-0.002, 0.002)
                lon = 77.6250 + random.uniform(-0.002, 0.002)
                search_logs.append(
                    models.SearchLog(
                        id=uuid.uuid4(),
                        query="momos",
                        latitude=lat,
                        longitude=lon,
                        created_at=day.replace(hour=random.randint(16, 21), minute=random.randint(0, 59))
                    )
                )

            # Searches for tea/chai near tech park / market (Lat: 12.9310 + noise, Lon: 77.6200 + noise)
            chai_searches = 20
            for _ in range(chai_searches):
                lat = 12.9310 + random.uniform(-0.002, 0.002)
                lon = 77.6200 + random.uniform(-0.002, 0.002)
                search_logs.append(
                    models.SearchLog(
                        id=uuid.uuid4(),
                        query="chai",
                        latitude=lat,
                        longitude=lon,
                        created_at=day.replace(hour=random.choices([9, 16])[0], minute=random.randint(0, 59))
                    )
                )

    print(f"Bulk saving {len(transactions)} transactions...")
    db.bulk_save_objects(transactions)
    print(f"Bulk saving {len(transaction_items)} transaction items...")
    db.bulk_save_objects(transaction_items)
    print(f"Bulk saving {len(expenses)} expenses...")
    db.bulk_save_objects(expenses)
    print(f"Bulk saving {len(search_logs)} search logs...")
    db.bulk_save_objects(search_logs)
    
    db.commit()
    print("Database seeding completed successfully!")
    db.close()

if __name__ == "__main__":
    seed_data()
