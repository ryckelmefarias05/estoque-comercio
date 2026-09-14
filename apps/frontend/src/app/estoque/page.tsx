import { getStock } from "../../services/stock-service";

export default async function StockPage() {
  const stockItems = await getStock();

  return (
    <main>
      <h1>Estoque</h1>

      <p>Total de produtos em estoque: {stockItems.length}</p>

      <div>
        {stockItems.map((item: Awaited<ReturnType<typeof getStock>>[number]) => (
          <div key={item.productId}>
            <h2>{item.productName}</h2>

            <p>SKU: {item.sku ?? "Não informado"}</p>

            <p>
              Código de barras:{" "}
              {item.barcode ?? "Não informado"}
            </p>

            <strong>
              Estoque: {item.quantity}
            </strong>
          </div>
        ))}
      </div>
    </main>
  );
}