import fs from 'node:fs/promises';
import path from 'node:path';
import { randomUUID } from 'node:crypto';
import pdf from 'pdf-parse/lib/pdf-parse.js';
import mammoth from 'mammoth';

export async function extractText(filePath, mimeType) {
  if (mimeType === 'application/pdf') {
    const result = await pdf(await fs.readFile(filePath));
    return result.text.trim();
  }
  const result = await mammoth.extractRawText({ path: filePath });
  return result.value.trim();
}

export function safeStoredName(filename) {
  const extension = path.extname(filename).toLowerCase();
  return `${randomUUID()}${extension}`;
}
