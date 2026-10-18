import multer from 'multer';

export function notFound(_req, res) { res.status(404).json({ error: 'Route not found.' }); }

export function errorHandler(error, _req, res, _next) {
  if (error instanceof multer.MulterError) {
    const message = error.code === 'LIMIT_FILE_SIZE' ? 'File exceeds the 10 MB limit.' : error.message;
    return res.status(400).json({ error: message });
  }
  if (error.status) return res.status(error.status).json({ error: error.message });
  console.error(error);
  return res.status(500).json({ error: 'An unexpected server error occurred.' });
}
