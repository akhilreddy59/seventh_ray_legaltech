import pool from '../db.js';

const transitions = { DRAFT: 'REVIEW', REVIEW: 'APPROVED' };

export async function listContracts() {
  const { rows } = await pool.query('SELECT id, title, file_name, status, created_at FROM contracts ORDER BY created_at DESC');
  return rows;
}

export async function getContract(id) {
  const { rows } = await pool.query('SELECT id, title, file_name, status, created_at, extracted_text FROM contracts WHERE id = $1', [id]);
  return rows[0] || null;
}

export async function createContract({ title, fileName, filePath, extractedText }) {
  const { rows } = await pool.query(
    'INSERT INTO contracts (title, file_name, file_path, extracted_text) VALUES ($1, $2, $3, $4) RETURNING id, title, file_name, status, created_at, extracted_text',
    [title, fileName, filePath, extractedText],
  );
  return rows[0];
}

export async function advanceContract(id) {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await client.query('SELECT status FROM contracts WHERE id = $1 FOR UPDATE', [id]);
    if (!result.rows[0]) { await client.query('ROLLBACK'); return { missing: true }; }
    const next = transitions[result.rows[0].status];
    if (!next) { await client.query('ROLLBACK'); return { invalid: true, current: result.rows[0].status }; }
    const { rows } = await client.query('UPDATE contracts SET status = $2, updated_at = NOW() WHERE id = $1 RETURNING id, title, file_name, status, created_at', [id, next]);
    await client.query('COMMIT');
    return { contract: rows[0] };
  } catch (error) { await client.query('ROLLBACK'); throw error; }
  finally { client.release(); }
}

export function answerQuestion(text, question) {
  const keywords = [...new Set((question.toLowerCase().match(/[\p{L}\p{N}]{3,}/gu) || []))];
  if (!keywords.length) return 'I could not find an answer in this document.';
  const sentences = text.split(/(?<=[.!?])\s+|\n+/).map((part) => part.trim()).filter(Boolean);
  const ranked = sentences.map((sentence, index) => ({ sentence, index, score: keywords.reduce((sum, word) => sum + (sentence.toLowerCase().includes(word) ? 1 : 0), 0) })).filter((row) => row.score > 0).sort((a, b) => b.score - a.score || a.index - b.index);
  return ranked[0]?.sentence || 'I could not find an answer in this document.';
}
