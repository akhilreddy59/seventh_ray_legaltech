import fs from 'node:fs/promises';
import path from 'node:path';
import { answerQuestion, advanceContract, createContract, getContract, listContracts } from '../services/contractService.js';
import { extractText, safeStoredName } from '../services/documentService.js';

const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const badRequest = (message) => Object.assign(new Error(message), { status: 400 });

export async function list(_req, res) { res.json({ contracts: await listContracts() }); }

export async function detail(req, res) {
  if (!uuidPattern.test(req.params.id)) return res.status(400).json({ error: 'A valid contract ID is required.' });
  const contract = await getContract(req.params.id);
  if (!contract) return res.status(404).json({ error: 'Contract not found.' });
  res.json({ contract });
}

export async function create(req, res, next) {
  let filePath;
  try {
    if (!req.file) throw badRequest('Choose a PDF or DOCX file to upload.');
    filePath = req.file.path;
    const title = typeof req.body.title === 'string' ? req.body.title.trim() : '';
    if (title.length > 200) throw badRequest('Title must be 200 characters or fewer.');
    let extractedText;
    try { extractedText = await extractText(filePath, req.file.mimetype); }
    catch { throw badRequest('This document could not be read. Please upload a valid PDF or DOCX file.'); }
    const contract = await createContract({ title: title || path.parse(req.file.originalname).name, fileName: req.file.originalname, filePath, extractedText });
    res.status(201).json({ contract });
  } catch (error) {
    if (filePath) await fs.unlink(filePath).catch(() => {});
    next(error);
  }
}

export async function advance(req, res) {
  if (!uuidPattern.test(req.params.id)) return res.status(400).json({ error: 'A valid contract ID is required.' });
  const result = await advanceContract(req.params.id);
  if (result.missing) return res.status(404).json({ error: 'Contract not found.' });
  if (result.invalid) return res.status(409).json({ error: `A contract in ${result.current} cannot move to another status.` });
  res.json({ contract: result.contract });
}

export async function question(req, res) {
  if (!uuidPattern.test(req.params.id)) return res.status(400).json({ error: 'A valid contract ID is required.' });
  if (typeof req.body?.question !== 'string' || !req.body.question.trim() || req.body.question.length > 500) return res.status(400).json({ error: 'Question must be between 1 and 500 characters.' });
  const contract = await getContract(req.params.id);
  if (!contract) return res.status(404).json({ error: 'Contract not found.' });
  res.json({ answer: answerQuestion(contract.extracted_text, req.body.question.trim()) });
}
