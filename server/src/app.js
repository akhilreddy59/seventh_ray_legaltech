import 'dotenv/config';
import express from 'express';
import cors from 'cors';
import contractRoutes from './routes/contractRoutes.js';
import { errorHandler, notFound } from './middleware/errorHandler.js';

const app = express();
app.use(cors({ origin: process.env.CLIENT_ORIGIN || 'http://localhost:5173' }));
app.use(express.json({ limit: '20kb' }));
app.get('/api/health', (_req, res) => res.json({ status: 'ok' }));
app.use('/api/contracts', contractRoutes);
app.use(notFound);
app.use(errorHandler);

const port = Number(process.env.PORT) || 3001;
app.listen(port, () => console.log(`Contract Manager API listening on port ${port}`));
