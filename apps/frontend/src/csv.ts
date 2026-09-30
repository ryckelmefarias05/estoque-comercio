export type ImportRow = { name: string; sku: string; barcode: string; unit: string; quantity: number };
export type Mapping = Record<keyof ImportRow, string>;
export const fields = { name: "Nome *", sku: "SKU / código do ERP", barcode: "Código de barras", unit: "Unidade (padrão UN)", quantity: "Saldo de referência *" };
export function parseCsv(text: string) {
  text = text.replace(/^\uFEFF/, "");
  const first = text.split(/\r?\n/, 1)[0];
  const separator = first.includes(";") ? ";" : ",";
  const records: string[][] = []; let record: string[] = [], cell = "", quoted = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (c === '"') {
      if (quoted && text[i + 1] === '"') { cell += '"'; i++; } else quoted = !quoted;
    } else if (!quoted && c === separator) { record.push(cell.trim()); cell = ""; }
    else if (!quoted && (c === "\n" || c === "\r")) {
      if (c === "\r" && text[i+1] === "\n") i++;
      record.push(cell.trim()); if (record.some(Boolean)) records.push(record); record = []; cell = "";
    } else cell += c;
  }
  if (quoted) throw new Error("CSV com aspas não fechadas.");
  record.push(cell.trim()); if (record.some(Boolean)) records.push(record);
  const headers = records.shift() || [];
  if (!headers.length || !records.length) throw new Error("O CSV precisa de cabeçalho e pelo menos um produto.");
  if (new Set(headers).size !== headers.length || headers.some(h => !h)) throw new Error("Cabeçalhos vazios ou repetidos.");
  if (records.length > 5000) throw new Error("Limite de 5.000 produtos por importação.");
  if (records.some(row => row.length !== headers.length)) throw new Error("Há linhas com quantidade de colunas diferente do cabeçalho.");
  return { headers, records };
}
const normalize = (s: string) => s.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase().replace(/[^a-z0-9]/g, "");
export function suggestMapping(headers: string[]): Mapping {
  const aliases = { name: ["nome", "produto", "descricao", "name"], sku: ["sku", "codigo", "codigoproduto", "codigoerp"], barcode: ["codigobarras", "codigodebarras", "barcode", "ean"], unit: ["unidade", "un", "unit"], quantity: ["quantidade", "saldo", "estoque", "quantity"] };
  return Object.fromEntries(Object.entries(aliases).map(([key, options]) => [key, headers.find(h => options.includes(normalize(h))) || ""])) as Mapping;
}
export function mapRows(headers: string[], records: string[][], mapping: Mapping): ImportRow[] {
  if (!mapping.name || !mapping.quantity || (!mapping.sku && !mapping.barcode)) throw new Error("Selecione nome, saldo e ao menos SKU ou código de barras.");
  const chosen = Object.values(mapping).filter(Boolean);
  if (new Set(chosen).size !== chosen.length) throw new Error("Cada campo precisa de uma coluna diferente.");
  return records.map((record, index) => {
    const get = (field: keyof Mapping) => record[headers.indexOf(mapping[field])] || "";
    let raw = get("quantity");
    if (!raw) throw new Error(`Linha ${index + 2}: saldo vazio.`);
    if (raw.includes(",")) raw = raw.replace(/\./g, "").replace(",", ".");
    if (!/^\d+(\.\d{1,3})?$/.test(raw)) throw new Error(`Linha ${index + 2}: saldo inválido (máximo 3 casas decimais).`);
    const quantity = Number(raw);
    if (quantity >= 1e11) throw new Error(`Linha ${index + 2}: saldo excede o limite.`);
    return { name: get("name"), sku: get("sku"), barcode: get("barcode"), unit: get("unit") || "UN", quantity };
  });
}
