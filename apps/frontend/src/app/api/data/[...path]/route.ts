import { NextRequest, NextResponse } from "next/server";
import { authHeader, readSession, sameOrigin } from "../../../../lib-auth";
async function handler(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  if (request.method !== "GET" && !sameOrigin(request)) return NextResponse.json({ message: "Origem inválida" }, { status: 403 });
  const session = readSession(request.cookies.get("vintra_session")?.value);
  if (!session) return NextResponse.json({ message: "Sessão encerrada. Entre novamente." }, { status: 401 });
  const { path } = await context.params;
  if (!path.length || !["me", "users", "imports", "tasks", "products", "stock", "inventory-counts"].includes(path[0]) || path.some(p => !/^[\w-]+$/.test(p))) return NextResponse.json({ message: "Rota inválida" }, { status: 404 });
  try {
    const upstream = await fetch(`${process.env.API_URL || "http://localhost:8081"}/api/${path.join("/")}`, {
      method: request.method,
      headers: { "content-type": "application/json", authorization: authHeader(session.email, session.password) },
      body: ["POST", "PUT", "PATCH"].includes(request.method) ? await request.text() : undefined,
      cache: "no-store", signal: AbortSignal.timeout(30000),
    });
    if (upstream.status === 401) return NextResponse.json({ message: "Credenciais expiradas ou alteradas. Entre novamente." }, { status: 401 });
    if (upstream.status === 403) return NextResponse.json({ message: "Seu perfil não tem permissão para esta operação." }, { status: 403 });
    return new NextResponse(upstream.status === 204 ? null : await upstream.text(), { status: upstream.status, headers: { "content-type": "application/json", "cache-control": "no-store" } });
  } catch { return NextResponse.json({ message: "API indisponível ou tempo de resposta excedido. Verifique o servidor antes de repetir a operação." }, { status: 502 }); }
}
export { handler as GET, handler as POST, handler as PUT, handler as PATCH };
