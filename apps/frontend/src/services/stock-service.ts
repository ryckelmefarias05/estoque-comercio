import type { StockItem } from "../types/stock";

const API_URL = "http://localhost:8081";

export async function getStock(): Promise<StockItem[]> {
  const response = await fetch(`${API_URL}/api/stock`, {
    cache: "no-store",
  });

  if (!response.ok) {
    throw new Error("Não foi possível carregar o estoque");
  }

  return response.json();
}   