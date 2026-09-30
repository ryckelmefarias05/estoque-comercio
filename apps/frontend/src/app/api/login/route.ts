import { NextRequest, NextResponse } from "next/server";
import { authHeader, makeSession, sameOrigin } from "../../../lib-auth";
export async function POST(request: NextRequest) {
  if (!sameOrigin(request)) return NextResponse.json({ message: "Origem inválida" }, { status: 403 });
  try {
    const { email, password } = await request.json();
    if (typeof email !== "string" || typeof password !== "string" || email.length > 160 || password.length > 100)
      return NextResponse.json({ message: "Informe e-mail e senha" }, { status: 400 });
    const upstream = await fetch(`${process.env.API_URL || "http://localhost:8081"}/api/me`, {
      headers: { authorization: authHeader(email.trim().toLowerCase(), password) }, cache: "no-store", signal: AbortSignal.timeout(10000),
    });
    if (upstream.status === 401 || upstream.status === 403) return NextResponse.json({ message: "E-mail ou senha inválidos" }, { status: 401 });
    if (!upstream.ok) return NextResponse.json({ message: "API indisponível. Verifique a inicialização do backend." }, { status: 502 });
    const session = makeSession(email.trim().toLowerCase(), password);
    const response = NextResponse.json(await upstream.json());
    response.cookies.set("vintra_session", session, { httpOnly: true, secure: process.env.NODE_ENV === "production", sameSite: "strict", path: "/", maxAge: 28800 });
    return response;
  } catch (error) {
    const configuration = error instanceof Error && error.message.includes("SESSION_SECRET");
    return NextResponse.json({ message: configuration ? "Configure SESSION_SECRET no frontend e reinicie o servidor." : "Não foi possível conectar à API. Inicie banco e backend." }, { status: 503 });
  }
}
