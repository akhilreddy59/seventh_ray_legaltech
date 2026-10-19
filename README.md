# Contract Manager

A minimal contract register with a React/Vite client and a Java 21 Spring Boot API. PostgreSQL stores contract metadata and extracted text; original files live in a private Supabase Storage bucket. The workflow supports `DRAFT → REVIEW → APPROVED` and keyword-based answers sourced from uploaded documents.

## Repository layout

- `client/` — React/Vite app; deploy this directory to Vercel.
- `server/` — Java 21/Spring Boot/Maven app; deploy this directory to Render.
- `render.yaml` — Render Blueprint for the `server` Docker service.

This is one repository with two separately deployed applications. Node.js and the package files under `client/` are required by Vite; there is no Node.js backend.

## Supabase setup

1. Create a Supabase project and a private Storage bucket named `contracts`.
2. Run [`server/src/main/resources/db/migration/V1__create_contracts.sql`](server/src/main/resources/db/migration/V1__create_contracts.sql) in the Supabase SQL Editor to create the schema manually. The same Flyway migration runs on application startup. Flyway baseline version `0` and idempotent DDL allow startup to record a schema created by this script.
3. Copy `server/.env.example` to `server/.env` for local development, then replace every `YOUR_...` value with your Supabase credentials. Use a TLS-enabled connection (`sslmode=require`). For Render, use the Supabase **session pooler** (port `5432`) if direct IPv6 connectivity is unavailable. Avoid transaction pooler port `6543` for this Flyway/JDBC service.
4. Keep the service-role key and database password in the server's environment only. Do not put them in Vercel variables, frontend files, or Git.

| Server variable | Purpose |
| --- | --- |
| `SUPABASE_DB_URL` | JDBC URL, e.g. `jdbc:postgresql://YOUR_SESSION_POOLER_HOST:5432/postgres?sslmode=require` |
| `SUPABASE_DB_USER` | PostgreSQL user; pooler usernames can include the project reference |
| `SUPABASE_DB_PASSWORD` | PostgreSQL password |
| `SUPABASE_URL` | Project URL, e.g. `https://PROJECT_REF.supabase.co` |
| `SUPABASE_SERVICE_ROLE_KEY` | Server-only Storage credential |
| `SUPABASE_STORAGE_BUCKET` | Private bucket name; defaults to `contracts` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed frontend origins, without paths |
| `PORT` | HTTP port; defaults to Render's `10000`; local `.env.example` sets it to `8080` |

There are no real credentials in the repository. Local file uploads are not used; files are stored in Supabase Storage.

## Local development

Requirements: Java 21, Maven, Node.js 20+, and npm. Set the server variables in your shell or IDE, then start the API:

```sh
cd server
mvn spring-boot:run
```

For PowerShell, load the example local environment file into the current process before running Maven:

```powershell
Get-Content .env | Where-Object { $_ -match '^[A-Za-z_][A-Za-z0-9_]*=' } | ForEach-Object {
  $name, $value = $_ -split '=', 2
  [Environment]::SetEnvironmentVariable($name, $value, 'Process')
}
mvn spring-boot:run
```

Copy `client/.env.example` to `client/.env` for local development. It points to the local Spring Boot API at `http://localhost:8080/api`. Then run the frontend:

```sh
cd client
npm install
npm run dev
```

All browser API requests use `VITE_API_BASE_URL`. Configure it at build time for each environment; production does not fall back to localhost.

## API

All endpoints return JSON; errors use `{ "error": "..." }`.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/health` | Health check |
| `POST` | `/api/contracts` | Multipart upload with `file` and optional `title` |
| `GET` | `/api/contracts` | List contracts |
| `GET` | `/api/contracts/{id}` | Contract details and extracted text |
| `PATCH` | `/api/contracts/{id}/status` | Advance one workflow step |
| `POST` | `/api/contracts/{id}/questions` | Ask a question about extracted text |

Uploads are limited to 10 MB and checked before parsing. Answers are selected from document sentences; they are not generated. Set `API_BASE` to the API URL ending in `/api` for the examples below.

```sh
curl -F "file=@agreement.pdf" -F "title=Services agreement" "$API_BASE/contracts"
curl "$API_BASE/contracts"
curl -X PATCH "$API_BASE/contracts/CONTRACT_UUID/status"
curl -X POST "$API_BASE/contracts/CONTRACT_UUID/questions" \
  -H "Content-Type: application/json" -d '{"question":"When does the agreement begin?"}'
```

## Deploy from Git

### Render — Spring Boot API

Create a Blueprint from this repository, or configure a Web Service with these exact settings:

| Setting | Value |
| --- | --- |
| Root Directory | `server` |
| Runtime | Docker |
| Dockerfile Path | `./Dockerfile` |
| Docker Context | `.` |
| Health Check Path | `/api/health` |

The Docker build uses Maven and Eclipse Temurin Java 21, then starts the executable Spring Boot jar. To build and run it locally from `server/`, use `mvn -DskipTests package` and `java -jar target/contract-manager-1.0.0.jar`.

Set these Render environment variables: `SUPABASE_DB_URL`, `SUPABASE_DB_USER`, `SUPABASE_DB_PASSWORD`, `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, and `CORS_ALLOWED_ORIGINS`. Set `SUPABASE_STORAGE_BUCKET=contracts`; Render supplies `PORT`. Mark credentials as secret. Set CORS to the exact deployed Vercel origin, for example `https://your-project.vercel.app`; add a custom domain origin if used. Do not include a path or trailing slash.

If Render logs `password authentication failed for user "postgres"`, check the three database values in Render's **Environment** settings. Copy the JDBC host and port from Supabase **Connect**. For the shared session pooler (port `5432`), set the username to `postgres.YOUR_PROJECT_REF`; for a direct database connection, use `postgres`. Set `SUPABASE_DB_PASSWORD` to the current database password from Supabase (reset it there if needed), entered separately from the JDBC URL. Redeploy after saving the corrected values. This credential error prevents Flyway and Spring Boot from starting, which also causes Render's “No open ports detected” message.

Flyway applies/records migrations during startup. The database schema script is also provided for manual setup in Supabase. This project uses persistent Supabase Storage for documents and does not depend on Render's ephemeral filesystem.

### Vercel — React/Vite client

Import the same repository as a separate Vercel project with:

| Setting | Value |
| --- | --- |
| Root Directory | `client` |
| Framework Preset | Vite |
| Build Command | `npm run build` |
| Output Directory | `dist` |
| Environment Variable | `VITE_API_BASE_URL=https://YOUR-RENDER-SERVICE.onrender.com/api` |

Replace `YOUR-RENDER-SERVICE` with the actual Render hostname and set this variable in Vercel for Production and any Preview/Development environments that should use the deployed API. It must include `/api`; Vite embeds this public API URL into the client bundle. Do not add Supabase credentials to Vercel. The committed `client/.env.example` is for local development; production API URL is configured in Vercel.

## Build and tests

Run from the repository root:

```sh
mvn -f server/pom.xml clean verify
cd client
npm ci
npm run build
```

This repository is configured for deployment but is not connected to live Supabase, Render, or Vercel projects. Deployment and the end-to-end hosted contract workflow have not been verified.
