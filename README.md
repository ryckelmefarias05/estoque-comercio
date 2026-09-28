# Vintra — controle operacional de estoque

Aplicação para catálogo de produtos, saldo, ajustes manuais, contagens e importação CSV de ajustes. Interface responsiva em Next.js, API Spring Boot e PostgreSQL.

## Execução local

Requisitos: Java 21, Node.js 22+, Docker Compose e npm. Use senhas próprias e não publique as variáveis de ambiente.

1. Copie `apps/backend/.env.example` e `apps/frontend/.env.example` para `.env` em cada pasta. Defina a mesma `VINTRA_API_USER` e `VINTRA_API_PASSWORD` nos dois arquivos. Defina `VINTRA_UI_PASSWORD` (senha do operador) e `SESSION_SECRET` aleatório de pelo menos 32 caracteres. Crie também `.env` na raiz com `DATABASE_PASSWORD` igual à do backend.
2. `docker compose up -d` na raiz. Se já existe volume PostgreSQL anterior, mantenha a senha existente no banco ou faça a troca diretamente no PostgreSQL: alterar a variável do contêiner não muda a senha de um volume já inicializado.
3. Na pasta `apps/backend`, carregue as variáveis de `.env` no ambiente e rode `./mvnw spring-boot:run` (Windows PowerShell: importe cada variável ou configure-as no sistema e rode `.\mvnw.cmd spring-boot:run`). API em `http://localhost:8081`.
4. Em `apps/frontend`, rode `npm ci` e `npm run dev`. Abra `http://localhost:3000`. O Next.js carrega seu `.env` automaticamente.

Para uma instância acessível ao cliente, use HTTPS, configure as variáveis no servidor e publique apenas a interface. Mantenha a API e o PostgreSQL em rede privada. Faça backup periódico do volume. Nenhum ambiente público está configurado neste repositório.

## Uso

- **Produtos:** cadastro, edição, busca, filtro e ajustes positivos/negativos. O indicador de saldo baixo usa limite geral de 5 unidades.
- **Contagens:** selecione produtos, registre as quantidades e finalize. Divergências são registradas; não alteram o estoque automaticamente.
- **Importar CSV:** exporte ou salve a planilha como CSV UTF-8 com cabeçalho `sku;ajuste` (também aceita vírgula como separador). Exemplo: `VIN-001;3` ou `VIN-002;-2`. Todos os SKUs precisam estar cadastrados. O painel valida linhas antes de aplicar. A importação faz chamadas individuais; se falhar no meio, os ajustes anteriores permanecem. Verifique os saldos antes de tentar novamente.

## Escopo atual e lacunas

O esquema SQL reserva tabelas para lotes, validade, avarias, recebimentos e tarefas, mas não há API nem interface funcional para esses fluxos. Portanto os indicadores desta versão são apenas de saldo. A autenticação atual usa uma senha compartilhada no painel e credencial de serviço na API; o esquema `users` ainda não é usado. Para vários clientes/empresas, é necessário isolamento por tenant, usuários com permissões, trilha de auditoria dos ajustes, importação atômica com idempotência, políticas de backup e testes de ponta a ponta. Não use uma instância compartilhada para dados de clientes diferentes.
