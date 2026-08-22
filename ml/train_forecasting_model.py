import os
import sys
import pandas as pd
import numpy as np
import xgboost as xgb
import joblib
from sqlalchemy import create_engine

# Bounding Box Boundaries for Koramangala
MIN_LAT, MAX_LAT = 12.9250, 12.9450
MIN_LON, MAX_LON = 77.6140, 77.6350
LAT_STEP = 0.002
LON_STEP = 0.0021

def get_grid_coordinates(lat, lon):
    if pd.isna(lat) or pd.isna(lon):
        return 0, 0
    grid_y = int((lat - MIN_LAT) / LAT_STEP)
    grid_x = int((lon - MIN_LON) / LON_STEP)
    # Clamp between 0 and 9
    grid_y = max(0, min(9, grid_y))
    grid_x = max(0, min(9, grid_x))
    return grid_y, grid_x

def train_model():
    # Database Connection
    DATABASE_URL = os.getenv("DATABASE_URL", "postgresql://postgres:password123@localhost:5432/streetfood_ai")
    engine = create_engine(DATABASE_URL)
    
    print("Loading transactions from database...")
    query = """
        SELECT t.total_amount, t.created_at, vp.last_latitude, vp.last_longitude
        FROM transactions t
        JOIN vendor_profiles vp ON t.vendor_profile_id = vp.id
    """
    df = pd.read_sql(query, engine)
    
    if df.empty:
        print("No transactions found. Seeding database first recommended.")
        return
        
    print(f"Loaded {len(df)} transactions. Processing features...")
    
    # 1. Feature Engineering: grid conversion
    grids = df.apply(lambda r: get_grid_coordinates(r['last_latitude'], r['last_longitude']), axis=1)
    df['grid_y'] = [g[0] for g in grids]
    df['grid_x'] = [g[1] for g in grids]
    
    # Convert total_amount to float
    df['total_amount'] = df['total_amount'].astype(float)
    
    # Extract temporal features
    df['created_at'] = pd.to_datetime(df['created_at'])
    df['hour'] = df['created_at'].dt.hour
    df['day_of_week'] = df['created_at'].dt.dayofweek
    
    # 2. Aggregate sales
    agg_df = df.groupby(['grid_y', 'grid_x', 'hour', 'day_of_week'])['total_amount'].sum().reset_index()
    agg_df.rename(columns={'total_amount': 'sales'}, inplace=True)
    
    # 3. Create all combinations grid to train with zeros (Negative Samples)
    # 10 x 10 grids, 24 hours, 7 days
    grid_ys = np.arange(10)
    grid_xs = np.arange(10)
    hours = np.arange(24)
    dows = np.arange(7)
    
    idx = pd.MultiIndex.from_product([grid_ys, grid_xs, hours, dows], names=['grid_y', 'grid_x', 'hour', 'day_of_week'])
    base_df = pd.DataFrame(index=idx).reset_index()
    
    # Merge aggregations into the full base dataset
    train_df = pd.merge(base_df, agg_df, on=['grid_y', 'grid_x', 'hour', 'day_of_week'], how='left')
    train_df['sales'] = train_df['sales'].fillna(0.0)
    
    # 4. Train Model
    X = train_df[['grid_y', 'grid_x', 'hour', 'day_of_week']]
    y = train_df['sales']
    
    print("Training XGBoost Regressor model...")
    model = xgb.XGBRegressor(
        n_estimators=100,
        max_depth=5,
        learning_rate=0.1,
        random_state=42
    )
    model.fit(X, y)
    
    # 5. Save model
    models_dir = os.path.join(os.path.dirname(__file__), 'models')
    os.makedirs(models_dir, exist_ok=True)
    model_path = os.path.join(models_dir, 'xgboost_demand.joblib')
    
    joblib.dump(model, model_path)
    print(f"Model saved successfully to {model_path}")

if __name__ == "__main__":
    train_model()
