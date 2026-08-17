# eLearning

Spring Boot e-learning backend/frontend app.

## Deployment

This repository already includes deployment configuration for multiple platforms:

- **Render**: use `render.yaml` (Blueprint deploy).
- **Railway**: use `railway.json` with the existing `Dockerfile`.
- **Docker Compose (self-hosted/local server)**: use `docker-compose.yml`.

### Quick deploy on Render

1. Create a Postgres database (for example Neon) and get:
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
2. In Render, create a **Blueprint** service from this repo.
3. Set the three datasource variables above.
4. Ensure `SPRING_PROFILES_ACTIVE=render-free`.
5. Deploy and verify `https://<service>.onrender.com/actuator/health` returns healthy.

### Quick deploy on Railway

1. Create a project and add a MySQL service.
2. Deploy this repo (uses `railway.json` + `Dockerfile`).
3. Set `SPRING_PROFILES_ACTIVE=railway`.
4. Ensure Railway DB env vars are available (`MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD`).
5. Verify `<railway-url>/actuator/health` returns healthy.
