import { NextRequest, NextResponse } from "next/server";
import { validSession } from "../../../../lib-auth";

async function handler(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  if (!validSession(request.cookies.get("vintra_session")?.value)) return NextResponse.json({ message: "Sessão do painel ausente ou expirada. Entre novamente." }, { status: 401 });
  const { path } = await context.params;
  if (!path.length || !["products", "stock", "inventory-counts"].includes(path[0]) || path.some(p => !/^[\w-]+$/.test(p))) return NextResponse.json({ message: "Rota inválida" }, { status: 404 });
  const url = process.env.API_URL || "http://localhost:8081";
  const user = process.env.VINTRA_API_USER, password = process.env.VINTRA_API_PASSWORD;
  if (!user || !password) return NextResponse.json({ message: "API não configurada" }, { status: 503 });
  try {
    const upstream = await fetch(`${url}/api/${path.join("/")}`, {
      method: request.method,
      headers: { "content-type": "application/json", authorization: `Basic ${Buffer.from(`${user}:${password}`).toString("base64")}` },
      body: ["POST", "PUT", "PATCH"].includes(request.method) ? await request.text() : undefined,
      cache: "no-store",
    });
    if (upstream.status === 401 || upstream.status === 403) {
      return NextResponse.json(
        { message: "A API recusou as credenciais de serviço. Confira se VINTRA_API_USER e VINTRA_API_PASSWORD são iguais no frontend e no backend; reinicie ambos." },
        { status: 502 },
      );
    }
    return new NextResponse(upstream.status === 204 ? null : await upstream.text(), { status: upstream.status, headers: { "content-type": "application/json" } });
  } catch {
    return NextResponse.json({ message: "API indisponível. Verifique o servidor e o banco de dados." }, { status: 502 });
  }
}
export { handler as GET, handler as POST, handler as PUT, handler as PATCH };
