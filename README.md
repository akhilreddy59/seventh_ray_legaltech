# Contract Manager

A small contract register built with React, Vite, Express, and PostgreSQL. It stores uploaded originals privately on the server, extracts text from PDF and DOCX files, enforces the `DRAFT → REVIEW → APPROVED` workflow, and answers questions by finding matching document sentences.

## Requirements

- Node.js 20 or newer
- PostgreSQL 13 or newer

## Setup

1. Create a PostgreSQL database, for example `contract_manager`.
2. Run `server/schema.sql` against that database.
3. In `server`, copy `.env.example` to `.env` and set `DATABASE_URL`. Set `UPLOAD_DIR` only if you want originals stored outside the default `server/uploads` folder.
4. In `client`, copy `.env.example` to `.env` if the API is not at `http://localhost:3001/api`.
5. Install and start each application in separate terminals:

   ```sh
   cd server
   npm install
   npm run dev
   ```

   ```sh
   cd client
   npm install
   npm run dev
   ```

Open the Vite URL (normally `http://localhost:5173`). The API listens on port 3001 by default. `CLIENT_ORIGIN` controls the allowed browser origin. Uploads are limited to 10 MB and are not served as static files.

## API

All endpoints return JSON. Errors use `{ "error": "..." }`.

| Method | Path | Notes |
| --- | --- | --- |
| `GET` | `/api/health` | Process health check; does not check database connectivity |
| `POST` | `/api/contracts` | Multipart form with `file` (PDF/DOCX) and optional `title` |
| `GET` | `/api/contracts` | List contracts, newest first |
| `GET` | `/api/contracts/:id` | Contract details and extracted text |
| `PATCH` | `/api/contracts/:id/status` | Advances one allowed workflow step; no client status field |
| `POST` | `/api/contracts/:id/questions` | JSON body: `{ "question": "When does it begin?" }` |

Example requests:

```sh
curl -F "file=@agreement.pdf" -F "title=Services agreement" http://localhost:3001/api/contracts
curl http://localhost:3001/api/contracts
curl -X PATCH http://localhost:3001/api/contracts/CONTRACT_UUID/status
curl -X POST http://localhost:3001/api/contracts/CONTRACT_UUID/questions \
  -H "Content-Type: application/json" -d '{"question":"When does the agreement begin?"}'
```

Question answering is intentionally literal: it ranks extracted sentences by how many question keywords they contain and returns the best match, or says that no answer was found. It does not interpret clauses or generate legal advice. The health endpoint confirms only that Express is responding; database readiness depends on `DATABASE_URL` and applying the schema.
