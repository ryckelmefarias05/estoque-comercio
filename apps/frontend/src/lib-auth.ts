import { createCipheriv, createDecipheriv, createHash, randomBytes } from "node:crypto";
export type Session = { email: string; password: string; expires: number };
function key() {
  const secret = process.env.SESSION_SECRET;
  if (!secret || secret.length < 32) throw new Error("Configure SESSION_SECRET com pelo menos 32 caracteres");
  return createHash("sha256").update(secret).digest();
}
// Credentials are encrypted server-side, never exposed in a readable browser cookie.
export function makeSession(email: string, password: string) {
  const iv = randomBytes(12);
  const cipher = createCipheriv("aes-256-gcm", key(), iv);
  const payload = JSON.stringify({ email, password, expires: Date.now() + 8 * 3600000 });
  const encrypted = Buffer.concat([cipher.update(payload, "utf8"), cipher.final()]);
  return Buffer.concat([iv, cipher.getAuthTag(), encrypted]).toString("base64url");
}
export function readSession(value?: string): Session | null {
  if (!value) return null;
  try {
    const data = Buffer.from(value, "base64url");
    const decipher = createDecipheriv("aes-256-gcm", key(), data.subarray(0, 12));
    decipher.setAuthTag(data.subarray(12, 28));
    const session = JSON.parse(Buffer.concat([decipher.update(data.subarray(28)), decipher.final()]).toString("utf8"));
    return typeof session.email === "string" && typeof session.password === "string" && session.expires > Date.now() ? session : null;
  } catch { return null; }
}
export function authHeader(email: string, password: string) { return `Basic ${Buffer.from(`${email}:${password}`).toString("base64")}`; }

export function sameOrigin(request: { headers: Headers }): boolean {
  const origin = request.headers.get("origin");
  const host = request.headers.get("host");
  try { return !!origin && !!host && new URL(origin).host === host; }
  catch { return false; }
}
