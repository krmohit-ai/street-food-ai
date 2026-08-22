import os
import joblib
import pandas as pd

# Bounding Box Boundaries for Koramangala
MIN_LAT, MAX_LAT = 12.9250, 12.9450
MIN_LON, MAX_LON = 77.6140, 77.6350
LAT_STEP = 0.002
LON_STEP = 0.0021

MODEL_PATH = os.path.join(os.path.dirname(__file__), 'models', 'xgboost_demand.joblib')
_model = None

def get_model():
    global _model
    if _model is None:
        if os.path.exists(MODEL_PATH):
            _model = joblib.load(MODEL_PATH)
        else:
            raise FileNotFoundError(f"XGBoost model not found at {MODEL_PATH}. Please run training first.")
    return _model

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

def forecast_grid_demand(grid_y: int, grid_x: int, hour: int, day_of_week: int) -> float:
    try:
        model = get_model()
        input_df = pd.DataFrame([{
            'grid_y': grid_y,
            'grid_x': grid_x,
            'hour': hour,
            'day_of_week': day_of_week
        }])
        prediction = model.predict(input_df)[0]
        return float(max(0.0, prediction))
    except Exception as e:
        print(f"Error during forecasting: {e}")
        return 0.0
