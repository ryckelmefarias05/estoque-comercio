import type { Metadata } from "next";
import "./globals.css";
export const metadata: Metadata = { title: "Vintra | Controle de estoque", description: "Painel operacional de estoque da adega" };
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) { return <html lang="pt-BR"><body>{children}</body></html>; }
