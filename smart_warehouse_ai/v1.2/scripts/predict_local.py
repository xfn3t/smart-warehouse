from app.data_builder import DataBuilder
from app.ml.model import ForecastModel

if __name__ == '__main__':
    b = DataBuilder()
    ds = b.build_dataset(days_back=120)
    fm = ForecastModel()
    pid = ds['product_id'].unique().tolist()[0]
    df = ds[ds['product_id']==pid].sort_values('date')
    preds = fm.predict_week(pid, df)
    print(preds)