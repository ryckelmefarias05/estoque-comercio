import type { StockItem } from "../types/stock";
export async function getStock(): Promise<StockItem[]> {
  const response = await fetch("/api/data/stock", { cache: "no-store" });
  if (!response.ok) throw new Error("Não foi possível carregar o estoque");
  return response.json();
}
