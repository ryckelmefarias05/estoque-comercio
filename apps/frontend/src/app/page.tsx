"use client";

import { useCallback, useEffect, useMemo, useState } from "react";

type Product = { id: number; name: string; sku: string | null; barcode: string | null; unit: string; active: boolean };
type Stock = { productId: number; quantity: number; updatedAt: string | null };
type Count = { id: number; status: string; createdAt: string; items: { id: number; productName: string; expectedQuantity: number; countedQuantity: number | null; differenceQuantity: number | null }[] };
type Tab = "overview" | "products" | "counts" | "imports";

async function api(path: string, method = "GET", body?: unknown) {
  const response = await fetch(`/api/data/${path}`, { method, headers: { "content-type": "application/json" }, body: body === undefined ? undefined : JSON.stringify(body) });
  if (response.status === 204) return null;
  const data = await response.json();
  if (response.status === 401) throw new Error(data.message || "Sessão encerrada. Entre novamente.");
  if (!response.ok) throw new Error(data.message || Object.values(data.fields || {}).join("; ") || "Operação não concluída");
  return data;
}
const qty = (n: number) => Number(n).toLocaleString("pt-BR", { maximumFractionDigits: 3 });

export default function Home() {
  const [authenticated, setAuthenticated] = useState(false);
  const [password, setPassword] = useState("");
  const [tab, setTab] = useState<Tab>("overview");
  const [products, setProducts] = useState<Product[]>([]);
  const [stocks, setStocks] = useState<Stock[]>([]);
  const [counts, setCounts] = useState<Count[]>([]);
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("all");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [editing, setEditing] = useState<Product | null>(null);
  const [form, setForm] = useState({ name: "", sku: "", barcode: "", unit: "UN" });
  const [selected, setSelected] = useState<number[]>([]);
  const [preview, setPreview] = useState<{ sku: string; adjustment: number; product?: Product; issue?: string }[]>([]);

  const load = useCallback(async () => {
    const [p, s, c] = await Promise.all([api("products"), api("stock"), api("inventory-counts")]);
    setProducts(p); setStocks(s); setCounts(c); setAuthenticated(true);
  }, []);
  useEffect(() => { const timer = setTimeout(() => { load().catch(() => setAuthenticated(false)); }, 0); return () => clearTimeout(timer); }, [load]);
  const stockMap = useMemo(() => new Map(stocks.map(s => [s.productId, s])), [stocks]);
  const active = products.filter(p => p.active);
  const visible = active.filter(p => {
    const value = `${p.name} ${p.sku || ""} ${p.barcode || ""}`.toLowerCase();
    const amount = Number(stockMap.get(p.id)?.quantity || 0);
    return value.includes(query.toLowerCase()) && (filter === "all" || (filter === "zero" ? amount === 0 : amount > 0 && amount <= 5));
  });
  async function run(action: () => Promise<void>) {
    setError(""); setMessage(""); setBusy(true);
    try { await action(); await load(); } catch (e) { setError(e instanceof Error ? e.message : "Erro inesperado"); }
    finally { setBusy(false); }
  }
  async function login(e: React.FormEvent) {
    e.preventDefault(); setError(""); setBusy(true);
    try { const r = await fetch("/api/login", { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify({ password }) }); if (!r.ok) throw new Error((await r.json()).message); await load(); setPassword(""); }
    catch (e) { setError(e instanceof Error ? e.message : "Falha no acesso"); } finally { setBusy(false); }
  }
  async function saveProduct(e: React.FormEvent) {
    e.preventDefault(); await run(async () => { await api(editing ? `products/${editing.id}` : "products", editing ? "PUT" : "POST", form); setMessage(editing ? "Produto atualizado." : "Produto cadastrado."); setEditing(null); setForm({ name: "", sku: "", barcode: "", unit: "UN" }); });
  }
  async function adjust(p: Product) {
    const input = prompt(`Ajuste de ${p.name}. Use valor positivo para entrada e negativo para saída:`, "1");
    if (input === null) return;
    const amount = Number(input.replace(",", "."));
    if (!Number.isFinite(amount) || amount === 0) { setError("Informe um ajuste numérico diferente de zero."); return; }
    if (!confirm(`Confirmar ajuste de ${qty(amount)} ${p.unit} para ${p.name}?`)) return;
    await run(async () => { await api(`stock/${p.id}/adjustment`, "PATCH", { adjustment: amount }); setMessage("Estoque atualizado."); });
  }
  async function importFile(file: File) {
    setError(""); setMessage("");
    if (!file.name.toLowerCase().endsWith(".csv")) { setError("Salve a planilha como CSV UTF-8 antes de importar."); return; }
    const text = (await file.text()).replace(/^\uFEFF/, "");
    const lines = text.trim().split(/\r?\n/);
    const separator = lines[0]?.includes(";") ? ";" : ",";
    const header = lines.shift()?.toLowerCase().split(separator).map(x => x.trim()) || [];
    if (header.join(",") !== "sku,ajuste") { setError("Cabeçalho esperado: sku;ajuste (ou sku,ajuste)."); return; }
    const seen = new Set<string>();
    setPreview(lines.filter(Boolean).map(line => {
      const [sku, raw, ...extra] = line.split(separator).map(x => x.trim().replace(/^"|"$/g, ""));
      const adjustment = Number(raw?.replace(",", "."));
      const product = active.find(p => p.sku?.toLowerCase() === sku?.toLowerCase());
      const issue = extra.length || !sku || !Number.isFinite(adjustment) || adjustment === 0 ? "Linha inválida" : seen.has(sku.toLowerCase()) ? "SKU duplicado" : !product ? "SKU não encontrado" : Number(stockMap.get(product.id)?.quantity || 0) + adjustment < 0 ? "Saldo ficaria negativo" : undefined;
      seen.add(sku?.toLowerCase());
      return { sku, adjustment, product, issue };
    }));
  }
  if (!authenticated) return <main className="login"><form onSubmit={login} className="login-card"><div className="brand">V<span>·</span></div><p className="eyebrow">VINTRA / OPERAÇÃO</p><h1>Seu estoque sob controle.</h1><p>Entre para acessar o painel da adega.</p><label>Senha de acesso<input autoFocus type="password" value={password} onChange={e => setPassword(e.target.value)} required /></label><button disabled={busy}>Entrar no painel →</button>{error && <p className="error">{error}</p>}</form></main>;
  return <div className="shell"><aside className="sidebar"><div className="logo">V<span>·</span> <small>VINTRA</small></div><div className="side-label">ÁREA DE TRABALHO</div><nav>{([ ["overview","Visão geral","◫"], ["products","Produtos","▦"], ["counts","Contagens","◷"], ["imports","Importar CSV","⇧"] ] as const).map(([id,label,icon]) => <button key={id} className={tab === id ? "active" : ""} onClick={() => setTab(id)}><b>{icon}</b>{label}</button>)}</nav><div className="side-bottom"><div className="online">● Sistema operacional</div><button onClick={async () => { await fetch("/api/logout", { method: "POST" }); setAuthenticated(false); }}>Sair da conta ↗</button></div></aside><main className="content"><header><div><p className="eyebrow">ADEGA / CONTROLE OPERACIONAL</p><h1>{({ overview: "Visão geral", products: "Produtos", counts: "Contagens", imports: "Importar estoque" })[tab]}</h1></div><div className="header-date">{new Intl.DateTimeFormat("pt-BR", { dateStyle: "full" }).format(new Date())}</div></header>{error && <div className="notice error">{error}</div>}{message && <div className="notice success">{message}</div>}
  {tab === "overview" && <><div className="hero"><div><p className="eyebrow">PAINEL DE ESTOQUE</p><h2>Informação clara.<br/>Decisão mais rápida.</h2><p>Acompanhe seus produtos e aja sobre os itens que precisam de atenção.</p></div><button onClick={() => setTab("products")}>Ver produtos →</button></div><div className="metrics"><div><span>Produtos ativos</span><strong>{active.length}</strong><small>cadastrados</small></div><div><span>Sem estoque</span><strong>{active.filter(p => !Number(stockMap.get(p.id)?.quantity || 0)).length}</strong><small>precisam de reposição</small></div><div><span>Estoque baixo</span><strong>{active.filter(p => { const n = Number(stockMap.get(p.id)?.quantity || 0); return n > 0 && n <= 5; }).length}</strong><small>até 5 unidades*</small></div><div><span>Contagens abertas</span><strong>{counts.filter(c => c.status !== "FINISHED").length}</strong><small>em andamento</small></div></div><section className="panel"><div className="panel-head"><div><h2>Itens que exigem atenção</h2><p>*Limite de 5 unidades usado como referência geral.</p></div></div><div className="rows">{active.filter(p => Number(stockMap.get(p.id)?.quantity || 0) <= 5).slice(0, 8).map(p => <div className="row" key={p.id}><div className="avatar">{p.name.slice(0,2).toUpperCase()}</div><div className="grow"><strong>{p.name}</strong><small>{p.sku || "Sem SKU"}</small></div><span className="badge danger">{qty(Number(stockMap.get(p.id)?.quantity || 0))} {p.unit}</span></div>)}{!active.length && <p className="empty">Cadastre o primeiro produto para começar.</p>}</div></section></>}
  {tab === "products" && <><div className="toolbar"><input placeholder="Buscar por nome, SKU ou código de barras" value={query} onChange={e => setQuery(e.target.value)} /><select value={filter} onChange={e => setFilter(e.target.value)}><option value="all">Todos os saldos</option><option value="zero">Sem estoque</option><option value="low">Estoque baixo (até 5)</option></select><button onClick={() => { setEditing(null); setForm({ name: "", sku: "", barcode: "", unit: "UN" }); document.getElementById("product-form")?.scrollIntoView({ behavior: "smooth" }); }}>+ Novo produto</button></div><section className="panel"><div className="panel-head"><h2>Catálogo <span className="muted">({visible.length})</span></h2></div><div className="table-wrap"><table><thead><tr><th>Produto</th><th>SKU</th><th>Código de barras</th><th>Saldo</th><th>Ações</th></tr></thead><tbody>{visible.map(p => <tr key={p.id}><td><strong>{p.name}</strong></td><td>{p.sku || "—"}</td><td>{p.barcode || "—"}</td><td><span className={`badge ${Number(stockMap.get(p.id)?.quantity || 0) <= 5 ? "danger" : "good"}`}>{qty(Number(stockMap.get(p.id)?.quantity || 0))} {p.unit}</span></td><td><div className="actions"><button onClick={() => adjust(p)}>Ajustar</button><button onClick={() => { setEditing(p); setForm({ name: p.name, sku: p.sku || "", barcode: p.barcode || "", unit: p.unit }); document.getElementById("product-form")?.scrollIntoView({ behavior: "smooth" }); }}>Editar</button></div></td></tr>)}</tbody></table>{!visible.length && <p className="empty">Nenhum produto encontrado.</p>}</div></section><section className="panel form-panel" id="product-form"><div className="panel-head"><h2>{editing ? `Editar ${editing.name}` : "Cadastrar produto"}</h2></div><form onSubmit={saveProduct} className="form-grid"><label>Nome *<input required maxLength={160} value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} /></label><label>SKU<input maxLength={80} value={form.sku} onChange={e => setForm({ ...form, sku: e.target.value })} /></label><label>Código de barras<input maxLength={80} value={form.barcode} onChange={e => setForm({ ...form, barcode: e.target.value })} /></label><label>Unidade<input maxLength={30} value={form.unit} onChange={e => setForm({ ...form, unit: e.target.value })} /></label><button disabled={busy}>{editing ? "Salvar alterações" : "Cadastrar produto"}</button>{editing && <button type="button" className="secondary" onClick={() => setEditing(null)}>Cancelar</button>}</form></section></>}
  {tab === "counts" && <><section className="panel"><div className="panel-head"><div><h2>Nova contagem</h2><p>Selecione os produtos que serão conferidos.</p></div><button disabled={!selected.length || busy} onClick={() => run(async () => { await api("inventory-counts", "POST", { productIds: selected }); setSelected([]); setMessage("Contagem iniciada."); })}>Iniciar ({selected.length})</button></div><div className="selection">{active.map(p => <label key={p.id}><input type="checkbox" checked={selected.includes(p.id)} onChange={e => setSelected(e.target.checked ? [...selected,p.id] : selected.filter(x => x !== p.id))} />{p.name}<small>saldo {qty(Number(stockMap.get(p.id)?.quantity || 0))}</small></label>)}</div></section><section className="panel"><div className="panel-head"><h2>Histórico de contagens</h2></div>{[...counts].reverse().map(c => <details className="count" key={c.id}><summary><strong>Contagem #{c.id}</strong><span>{new Date(c.createdAt).toLocaleDateString("pt-BR")}</span><span className="badge">{c.status === "FINISHED" ? "Finalizada" : "Em andamento"}</span></summary>{c.items.map(item => <div className="count-item" key={item.id}><div><strong>{item.productName}</strong><small>Esperado: {qty(item.expectedQuantity)} · Contado: {item.countedQuantity === null ? "pendente" : qty(item.countedQuantity)}</small></div>{c.status !== "FINISHED" && <button disabled={busy} onClick={() => { const value = prompt(`Quantidade conferida de ${item.productName}:`, String(item.countedQuantity ?? item.expectedQuantity)); if (value === null) return; const number = Number(value.replace(",", ".")); if (!Number.isFinite(number) || number < 0) { setError("Informe uma quantidade válida."); return; } run(async () => { await api(`inventory-counts/${c.id}/items/${item.id}`, "PATCH", { countedQuantity: number }); setMessage("Quantidade registrada."); }); }}>Registrar</button>}</div>)}{c.status !== "FINISHED" && <button className="finish" disabled={busy || c.items.some(i => i.countedQuantity === null)} onClick={() => run(async () => { await api(`inventory-counts/${c.id}/finish`, "PATCH"); setMessage("Contagem finalizada. Divergências não alteram o estoque automaticamente."); })}>Finalizar contagem</button>}</details>)}{!counts.length && <p className="empty">Nenhuma contagem iniciada.</p>}</section></>}
  {tab === "imports" && <><div className="hero import-hero"><div><p className="eyebrow">INTEGRAÇÃO MANUAL</p><h2>Importação por CSV</h2><p>Exporte do ERP e revise cada ajuste antes de aplicá-lo ao estoque.</p></div></div><section className="panel"><div className="panel-head"><div><h2>Arquivo de ajustes</h2><p>CSV UTF-8 com colunas <code>sku;ajuste</code>. Valores positivos entram, negativos saem. Excel: use “Salvar como CSV”.</p></div></div><div className="upload"><input type="file" accept=".csv,text/csv" onChange={e => { const file = e.target.files?.[0]; if (file) importFile(file); }} /><p>Exemplo: <code>sku;ajuste</code> / <code>VIN-001;3</code> / <code>VIN-002;-2</code></p></div>{preview.length > 0 && <><div className="table-wrap"><table><thead><tr><th>SKU</th><th>Produto</th><th>Ajuste</th><th>Validação</th></tr></thead><tbody>{preview.map((row,i) => <tr key={i}><td>{row.sku}</td><td>{row.product?.name || "—"}</td><td>{qty(row.adjustment)}</td><td><span className={`badge ${row.issue ? "danger" : "good"}`}>{row.issue || "Pronto"}</span></td></tr>)}</tbody></table></div><div className="import-actions"><button disabled={busy || preview.some(r => r.issue)} onClick={() => { if (!confirm(`Aplicar ${preview.length} ajustes? Essa operação altera o estoque.`)) return; run(async () => { let applied = 0; for (const row of preview) { try { await api(`stock/${row.product!.id}/adjustment`, "PATCH", { adjustment: row.adjustment }); applied++; } catch (e) { throw new Error(`${applied} de ${preview.length} ajustes aplicados. Falha no SKU ${row.sku}: ${e instanceof Error ? e.message : "erro"}. Verifique os saldos antes de tentar novamente.`); } } setPreview([]); setMessage(`${applied} ajustes aplicados.`); }); }}>Aplicar {preview.length} ajustes</button><button className="secondary" onClick={() => setPreview([])}>Descartar</button></div></>}</section></>}
  <footer>VINTRA · Controle operacional de estoque <span>Dados vinculados à API da adega</span></footer></main><div className="mobile-nav">{([ ["overview","Início"], ["products","Produtos"], ["counts","Contagens"], ["imports","Importar"] ] as const).map(([id,label]) => <button className={tab === id ? "active" : ""} key={id} onClick={() => setTab(id)}>{label}</button>)}</div></div>;
}
