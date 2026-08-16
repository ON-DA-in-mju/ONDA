export default function handler(req, res) {
  if (req.method !== 'GET') {
    res.status(405).json({ ok: false, message: 'Method Not Allowed' })
    return
  }
  res.status(200).json([])
}
