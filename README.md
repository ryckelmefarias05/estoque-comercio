# Adega Estoque

Sistema web para controle operacional de estoque.

## Objetivo

Auxiliar a operação diária do estoque da Adega, permitindo:

- Controle de quantidade
- Inventário
- Conferência de recebimento
- Importação de NF-e via XML
- Controle de lote
- Controle de validade
- Registro de avarias
- Controle de qualidade
- Organização do estoque
- Gestão de tarefas
- Acompanhamento de produtividade
- Registro de divergências
- Exportação de ajustes para o ERP

## Arquitetura

- Backend: Java + Spring Boot
- Frontend: Next.js + React + TypeScript
- Banco de dados: PostgreSQL
- ORM: Spring Data JPA / Hibernate
- Migrations: Flyway
- API: REST
- Infraestrutura: Docker
- Versionamento: Git + GitHub

## Integração ERP

Na primeira versão, a integração com o FlyPDV Saurus será realizada através de importação e exportação de arquivos CSV/Excel.

Uma integração via API poderá ser adicionada futuramente.
