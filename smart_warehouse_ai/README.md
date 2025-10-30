# Start model

## Install requirements dependency's

```shell
pip install -r requirements.txt
```

## Generate dataset

```shell
python ai_dataset_generator.py
```

Dataset size set in function call

```shell
if __name__ == "__main__":
    generate_inventory_dataset(1000)
```


## Train model

```shell
python ai_train_model.py
```

## Up FastAPI service

```shell
uvicorn ai_service:app --reload --port 8001
```