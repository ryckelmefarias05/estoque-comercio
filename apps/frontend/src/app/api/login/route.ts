import { NextRequest, NextResponse } from "next/server";
import { timingSafeEqual } from "node:crypto";
import { makeSession } from "../../../lib-auth";
export async function POST(request: NextRequest) {
  const { password } = await request.json();
  const expected = process.env.VINTRA_UI_PASSWORD;
  if (!expected || expected.length < 16 || typeof password !== "string") return NextResponse.json({ message: "Acesso indisponível" }, { status: 503 });
  const a = Buffer.from(password), b = Buffer.from(expected);
  if (a.length !== b.length || !timingSafeEqual(a, b)) return NextResponse.json({ message: "Senha inválida" }, { status: 401 });
  const response = NextResponse.json({ ok: true });
  response.cookies.set("vintra_session", makeSession(), { httpOnly: true, secure: process.env.NODE_ENV === "production", sameSite: "strict", path: "/", maxAge: 28800 });
  return response;
}
