import { createHmac, timingSafeEqual } from "node:crypto";

const key = () => {
  const secret = process.env.SESSION_SECRET;
  if (!secret || secret.length < 32) throw new Error("SESSION_SECRET deve ter pelo menos 32 caracteres");
  return secret;
};
export function makeSession() {
  const expires = String(Date.now() + 8 * 60 * 60 * 1000);
  return `${expires}.${createHmac("sha256", key()).update(expires).digest("hex")}`;
}
export function validSession(value?: string) {
  if (!value) return false;
  const [expires, signature] = value.split(".");
  if (!expires || !signature || !/^\d+$/.test(expires) || Number(expires) < Date.now()) return false;
  const expected = createHmac("sha256", key()).update(expires).digest("hex");
  return signature.length === expected.length && timingSafeEqual(Buffer.from(signature), Buffer.from(expected));
}
