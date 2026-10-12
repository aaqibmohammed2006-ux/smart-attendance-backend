# Smart Attendance Backend

Spring Boot REST API for the Smart Attendance Management System.

## Run locally

Requires Java 21 and Maven. From this repository:

```powershell
mvn spring-boot:run
```

The API uses an in-memory H2 database by default and listens on `http://localhost:8080`. Fictional demo records are seeded only when the default or `dev` profile is active. Data in the default H2 database is not persistent across application restarts.

## API and health check

The API base path is `/api`. The Render health-check endpoint is `GET /api/health`; it verifies both the application and its configured database connection. Other primary routes include:

- `/api/students`, `/api/subjects`, `/api/enrollments`
- `/api/sessions`, `/api/attendance/session/{sessionId}`
- `/api/dashboard`, `/api/reports`

## Deploy this repository to Render

1. Push this folder as the root of the `smart-attendance-backend` GitHub repository.
2. In Render, create a **Web Service** and connect that repository. Choose **Docker** as the runtime. Set the Root Directory to `.` and Dockerfile path to `./Dockerfile`. Render builds the Java 21 application with Maven in the Dockerfile; no separate build or start command is required.
3. Create a Render PostgreSQL database. In the database dashboard, copy its **internal** connection details. Configure the web service with:
   - `SPRING_PROFILES_ACTIVE=postgres`
   - `SPRING_DATASOURCE_URL=jdbc:postgresql://<internal-host>:<port>/<database-name>`
   - `SPRING_DATASOURCE_USERNAME=<database-user>`
   - `SPRING_DATASOURCE_PASSWORD=<database-password>`
   - `CORS_ALLOWED_ORIGINS=https://<your-frontend-service>.onrender.com`
4. Set the Render health check path to `/api/health`. Render supplies the service port in `PORT`; Spring Boot binds to it.
5. Deploy, then verify `https://<your-backend-service>.onrender.com/api/health` returns HTTP 200 and `{"status":"UP","database":"UP"}`.

The `postgres` profile requires the three `SPRING_DATASOURCE_*` variables, disables H2 and demo seeding, and connects to PostgreSQL. Hibernate uses `ddl-auto=update` to create/update the current entity schema in the persistent database; back up production data before deploying entity changes. Do not use an ephemeral or in-memory database for production.

For local PostgreSQL profile testing, copy `.env.example` to `.env` and set the password locally; never commit `.env`.

Render service prices, database availability, sleep behavior, storage, and limits change over time. Check Render's current pricing and service limitations before choosing a plan: https://render.com/pricing and https://render.com/docs/free

## Tests

```powershell
mvn test -q
```
