import { Router } from 'express';
import multer from 'multer';
import path from 'node:path';
import fs from 'node:fs';
import { advance, create, detail, list, question } from '../controllers/contractController.js';
import { safeStoredName } from '../services/documentService.js';

const router = Router();
const asyncRoute = (handler) => (req, res, next) => Promise.resolve(handler(req, res, next)).catch(next);
const uploadDir = path.resolve(process.env.UPLOAD_DIR || 'uploads');
fs.mkdirSync(uploadDir, { recursive: true });
const allowed = new Set(['application/pdf', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document']);
const upload = multer({
  storage: multer.diskStorage({ destination: uploadDir, filename: (_req, file, done) => done(null, safeStoredName(file.originalname)) }),
  limits: { fileSize: 10 * 1024 * 1024, files: 1 },
  fileFilter: (_req, file, done) => {
    const ext = path.extname(file.originalname).toLowerCase();
    if ((ext === '.pdf' && file.mimetype === 'application/pdf') || (ext === '.docx' && file.mimetype === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document')) return done(null, true);
    done(Object.assign(new Error('Only PDF and DOCX files are supported.'), { status: 400 }));
  },
});

router.get('/', asyncRoute(list));
router.post('/', upload.single('file'), asyncRoute(create));
router.get('/:id', asyncRoute(detail));
router.patch('/:id/status', asyncRoute(advance));
router.post('/:id/questions', asyncRoute(question));
export default router;
