import test from "node:test";
import assert from "node:assert/strict";
import { parseCsv, suggestMapping, mapRows } from "../src/csv.ts";
import { makeSession, readSession } from "../src/lib-auth.ts";

test("CSV preserves leading zeros, quoted delimiters and pt-BR decimals", () => {
  const { headers, records } = parseCsv('\uFEFFnome;sku;codigo_barras;unidade;quantidade\r\n"Água; mineral";0001;001234;UN;"1.234,500"\r\n');
  assert.deepEqual(mapRows(headers, records, suggestMapping(headers)), [{ name: "Água; mineral", sku: "0001", barcode: "001234", unit: "UN", quantity: 1234.5 }]);
});
test("CSV accepts comma delimiters, escaped quotes and multiline names", () => {
  const { headers, records } = parseCsv('nome,sku,quantidade\n"Produto ""A""\n500ml",0001,3');
  assert.equal(mapRows(headers, records, suggestMapping(headers))[0].name, 'Produto "A"\n500ml');
});
test("Missing stock is not silently converted to zero", () => {
  const { headers, records } = parseCsv("nome;sku;quantidade\nProduto;A;");
  assert.throws(() => mapRows(headers, records, suggestMapping(headers)), /saldo vazio/);
});
test("Invalid precision, negative numbers, broken CSV and ambiguous mappings are rejected", () => {
  for (const quantity of ["-1", "1.0001", "texto"]) {
    const { headers, records } = parseCsv(`nome;sku;quantidade\nProduto;A;${quantity}`);
    assert.throws(() => mapRows(headers, records, suggestMapping(headers)));
  }
  assert.throws(() => parseCsv('nome;sku\n"Produto;A'), /aspas/);
  assert.throws(() => parseCsv("nome;sku\nProduto;A;3"), /colunas/);
  const { headers, records } = parseCsv("nome;sku;quantidade\nProduto;A;3");
  assert.throws(() => mapRows(headers, records, { ...suggestMapping(headers), barcode: "sku" }), /diferente/);
});
test("Session credentials are encrypted and invalidated by tampering or key rotation", () => {
  process.env.SESSION_SECRET = "test-only-key-with-at-least-32-characters";
  const cookie = makeSession("operator@example.test", "password-never-in-plaintext");
  assert.equal(readSession(cookie)?.email, "operator@example.test");
  assert.ok(!Buffer.from(cookie, "base64url").toString().includes("password-never-in-plaintext"));
  const bytes = Buffer.from(cookie, "base64url"); bytes[30] ^= 1;
  assert.equal(readSession(bytes.toString("base64url")), null);
  process.env.SESSION_SECRET = "rotated-test-key-with-at-least-32-characters";
  assert.equal(readSession(cookie), null);
  assert.equal(readSession("old-format.invalid"), null);
});
