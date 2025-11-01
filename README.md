# Start Prod

```shell
docker-compose --env-file .env.dev up --build -d
```
# Start TestDB
```shell
docker compose -f docker-compose.test.yaml up -d
```