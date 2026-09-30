# Vintra — gestão da rotina de estoque

O Vintra é um SaaS operacional que organiza o trabalho do estoque do comércio: o gestor traz a base de produtos do ERP, define o que precisa ser conferido e distribui o trabalho. Cada operador acessa sua própria fila, registra a contagem física e informa o resultado das tarefas. O gestor acompanha execução e divergências.

O FlyPDV/Saurus continua sendo o ERP do comércio. Nesta versão a integração é manual por arquivo; o Vintra não consulta nem altera o FlyPDV por API.

## Fluxo principal

1. O gestor entra com seu usuário e cadastra os operadores.
2. Exporta a base do ERP em CSV UTF-8 (uma planilha Excel deve ser salva nesse formato).
3. Seleciona o arquivo, relaciona as colunas e escolhe o operador responsável.
4. O Vintra mostra uma prévia de produtos existentes e novos. Confirmação cadastra os novos e cria uma contagem com as quantidades do CSV como referência.
5. O operador entra com e-mail e senha próprios. Vê somente as contagens e tarefas atribuídas a ele.
6. Registra as quantidades físicas e observações. A diferença é calculada como quantidade contada menos quantidade de referência.
7. Finaliza a contagem após salvar todos os itens. O gestor analisa os resultados e decide quais ajustes devem ser feitos.

Importar 24 unidades significa “o ERP informou 24 na data da importação”. Não significa receber mais 24 unidades. Importação e finalização de contagem **não alteram automaticamente o saldo operacional do Vintra**, nem o saldo do ERP. Isso evita duplicar estoque ao importar uma nova fotografia da base. Ajustes manuais ficam na área Produtos do gestor.

## Perfis e permissões

| Recurso | Gestor (ADMIN) | Operador (OPERATOR) |
|---|---|---|
| Cadastrar operadores | Sim | Não |
| Importar e consultar histórico de arquivos | Sim | Não |
| Cadastrar/editar produtos e ajustar saldo | Sim | Não |
| Criar e atribuir contagens | Sim | Não |
| Reatribuir contagem ainda aberta | Sim | Não |
| Ver contagens e divergências | Todas | Somente as atribuídas |
| Registrar e finalizar contagem | Sim | Somente as atribuídas |
| Criar tarefas e definir prazo | Sim | Não |
| Iniciar/concluir tarefas e registrar observações | Todas | Somente as atribuídas |

As restrições são aplicadas na API. Esconder botões no frontend não é a proteção: tentar acessar diretamente o ID da contagem de outro operador também é recusado.

## Importação: regras e formato

Arquivo de exemplo: [docs/exemplos/produtos-erp.csv](docs/exemplos/produtos-erp.csv).

```csv
nome;sku;codigo_barras;unidade;quantidade
Água mineral 500ml;AGUA-500;7890000000001;UN;24
Refrigerante 2L;REFRI-2L;7890000000002;UN;12
Gelo 5kg;GELO-5KG;;UN;8
```

- CSV UTF-8, delimitado por ponto e vírgula ou vírgula; cabeçalho obrigatório.
- Aspas, aspas escapadas, campos contendo delimitadores e quebras de linha são suportados.
- Limite na interface: 5 MB e 5.000 produtos. A API limita a 5.000 linhas por requisição.
- A tela sugere o mapeamento de nome/descrição, SKU/código, EAN/código de barras, unidade e saldo/quantidade. O gestor pode trocar o mapeamento.
- Nome e quantidade são obrigatórios. Cada linha deve ter SKU ou código de barras. Unidade omitida usa `UN`.
- Identificadores são tratados como texto: `000123` mantém os zeros à esquerda. A comparação no banco é exata e diferencia maiúsculas/minúsculas. Mantenha o padrão do ERP.
- Quantidades não negativas, até 11 dígitos inteiros e 3 casas decimais. Aceita decimal `12.5` ou `12,5` e formato brasileiro `1.234,500`. Sem vírgula, ponto é decimal. `1,234.50` não é formato aceito.
- Produto existente é identificado pelo SKU ou código de barras; se ambos apontarem para cadastros diferentes, a importação é recusada. Não há associação automática por nome.
- Produtos novos são cadastrados automaticamente na confirmação. Produtos existentes preservam nome, unidade e identificadores já cadastrados; unidade incompatível ou identificadores conflitantes causam erro.
- Identificadores/produtos repetidos no mesmo arquivo e produtos inativos são recusados. Um produto por linha; não há agrupamento automático de lotes.
- Prévia não grava dados. Confirmação grava produtos, contagem e histórico em uma única transação PostgreSQL: falha em qualquer linha desfaz a confirmação inteira.
- Cada seleção de arquivo recebe uma chave de importação. Repetir a confirmação com essa chave reutiliza o resultado, inclusive após falha de rede. Selecionar o arquivo novamente gera uma nova chave e uma nova contagem; não duplica cadastros nem soma saldo.

A contagem guarda a quantidade de referência da importação. Alterar o saldo posteriormente não muda essa referência. O arquivo original e seu conteúdo bruto não são arquivados; o histórico registra nome do arquivo, autor, horário, quantidade de linhas, novos produtos e contagem gerada. Guarde o CSV original quando precisar de evidência externa.

## Contagens e tarefas

**Contagens:** `OPEN → IN_PROGRESS → FINISHED`. Salvar o primeiro item inicia a contagem. Itens podem ser corrigidos enquanto estiver aberta. Não é possível finalizar com itens pendentes nem editar uma contagem finalizada. Zero é uma contagem válida. Campos exibem referência, contado e diferença; não se trata de contagem cega.

Contagens criadas manualmente usam o saldo atual do Vintra. Contagens criadas pelo CSV usam o saldo do arquivo. Contagens anteriores a esta atualização continuam disponíveis ao gestor; as abertas sem operador podem ser atribuídas na tela.

**Tarefas:** `PENDING → IN_PROGRESS → COMPLETED`. O gestor informa título, instruções, responsável, tipo e prazo opcional. Tipos: organização, verificação de validade, qualidade, conferência de recebimento e outros. O operador registra observações ao iniciar/concluir. O sistema registra horários de início e término. O prazo é informado e exibido no horário local da operação; use servidores e usuários com fuso coerente.

Uma tarefa de “verificar validade” registra trabalho e observações. Ela não cria um cadastro estruturado de lote/validade, nem uma entrada de mercadoria.

## Atualização da versão anterior (patch)

Base do patch: commit `b47b707` de `main`. Ele já contém o painel inicial e a correção das mensagens de autenticação.

1. Confirme que o trabalho anterior foi commitado (`git status` limpo).
2. Pare backend e frontend.
3. Faça backup do PostgreSQL antes de aplicar a migration. Na raiz, com o banco rodando:

   ```powershell
   docker compose exec postgres pg_dump -U adega -d adega_estoque -Fc -f /tmp/vintra-before-update.dump
   docker compose cp postgres:/tmp/vintra-before-update.dump "$HOME\Downloads\vintra-before-update.dump"
   ```

4. Na raiz do repositório, aplique:

   ```powershell
   git apply --check "$HOME\Downloads\vintra-importacao-operador.patch"
   git apply "$HOME\Downloads\vintra-importacao-operador.patch"
   ```

   Se o primeiro comando apontar conflito, não execute o segundo; envie a saída para conciliar a versão local.

5. Atualize os `.env` conforme abaixo, mantendo sua senha atual do banco e a chave de sessão.
6. Abra terminais novos e inicie banco, backend e frontend nessa ordem.
7. O Flyway aplica `V2__operational_imports.sql`: adiciona observações de tarefas, histórico de importações e índices. Não apaga produtos, saldos ou contagens existentes. Não altere a migration V1 nem use `docker compose down -v`.
8. Entre com o novo e-mail/senha de gestor, cadastre um operador e siga o roteiro de validação.

**Mudança de login:** o acesso por uma senha compartilhada foi substituído por usuários no PostgreSQL. `VINTRA_UI_PASSWORD`, `VINTRA_API_USER` e `VINTRA_API_PASSWORD` não são mais usados. Sessões antigas deixam de ser válidas; entre novamente.

## Configuração e execução local

Requisitos: Java 21, Node.js 22.20+ (ou 24), npm e Docker Compose. O PostgreSQL fica na porta 5433; a API na 8081, vinculada a 127.0.0.1 por padrão. Em uma rede privada de implantação, configure SERVER_ADDRESS conforme necessário. O Next usa 3000 ou a porta disponível informada no terminal.

### 1. Banco: `.env` na raiz

```dotenv
DATABASE_PASSWORD=SUA_SENHA_ATUAL_DO_POSTGRES
```

O Docker lê esse arquivo. Em um volume já inicializado, a senha real do banco não muda ao editar o arquivo.

```powershell
docker compose up -d
docker compose ps
```

### 2. Backend: `apps/backend/.env`

```dotenv
DATABASE_URL=jdbc:postgresql://localhost:5433/adega_estoque
DATABASE_USER=adega
DATABASE_PASSWORD=SUA_SENHA_ATUAL_DO_POSTGRES
VINTRA_ADMIN_EMAIL=seu-email@exemplo.com
VINTRA_ADMIN_PASSWORD=SUA_SENHA_FORTE_DE_GESTOR
```

O backend agora importa `.env` automaticamente como propriedades ao iniciar **a partir de `apps/backend`**. Use `CHAVE=valor`, sem aspas, comentários somente em linhas separadas e senhas ASCII sem barras invertidas. Variáveis já exportadas no sistema/terminal têm precedência; abra um terminal novo se usava o carregamento manual.

O gestor inicial é criado somente se não houver gestor ativo no banco. Senha mínima de 16 caracteres e máxima de 72 bytes, armazenada com BCrypt. Alterar `VINTRA_ADMIN_PASSWORD` depois da criação não redefine a senha de um usuário existente. Esta versão não tem recuperação de senha por e-mail.

```powershell
cd apps\backend
.\mvnw.cmd spring-boot:run
```

Espere `Started EstoqueApplication`. Se aparecer erro SCRAM sem senha, confira a pasta de execução e `DATABASE_PASSWORD`. Se aparecer `password authentication failed`, a senha enviada difere da senha real do PostgreSQL.

### 3. Frontend: `apps/frontend/.env`

```dotenv
API_URL=http://localhost:8081
SESSION_SECRET=SUA_CHAVE_ALEATORIA_COM_PELO_MENOS_32_CARACTERES
```

Não use prefixo `NEXT_PUBLIC_` nessas variáveis. O Next lê `.env` automaticamente. Remova definições antigas conflitantes de `.env.local` ou do ambiente.

Em outro terminal:

```powershell
cd apps\frontend
npm ci
npm run dev
```

Abra a URL exibida e use `VINTRA_ADMIN_EMAIL` / `VINTRA_ADMIN_PASSWORD` do primeiro cadastro. O gestor cria os acessos dos operadores pela aba Operadores. O papel é retornado pela API e a interface abre a área correspondente.

As sessões duram 8 horas. O cookie é HttpOnly, SameSite Strict e criptografado com AES-GCM; o servidor Next usa as credenciais da sessão para autenticar cada chamada no Spring. Trocar a chave de sessão encerra as sessões existentes. Em produção o cookie exige HTTPS. Para uso local, use `npm run dev`.

## Validação antes de usar com dados reais

Com banco de testes separado e variáveis configuradas:

```powershell
# apps/frontend
npm test
npm run lint
npm run build

# apps/backend (exige PostgreSQL e Java 21)
.\mvnw.cmd test
```

Os testes de backend usam o banco configurado. Os dados de teste são revertidos/removidos; migrations e o bootstrap inicial podem persistir. Não execute a suíte contra o banco do cliente.

Roteiro manual:

1. Entre como gestor e crie dois operadores.
2. Importe o CSV de exemplo para o primeiro operador. Confira três novos produtos na prévia e uma contagem após confirmar.
3. Selecione o mesmo arquivo novamente: os três produtos devem ser reconhecidos como existentes; uma nova contagem representa uma nova conferência, sem somar saldo.
4. Tente importar linha sem identificador, unidade incompatível ou produto duplicado: a operação deve ser recusada sem gravação parcial.
5. Entre como o primeiro operador, conte os itens, salve e finalize. Confira as diferenças no gestor.
6. Entre como o segundo operador: ele não pode ver nem editar a contagem do primeiro, inclusive pela API.
7. Distribua uma tarefa de organização, inicie e conclua como responsável, registrando observações.
8. Recarregue as páginas e reinicie a aplicação para verificar persistência.

Nesta entrega, build/lint e testes de CSV/criptografia do frontend foram executados. A suíte Spring/PostgreSQL precisa ser executada no ambiente acima: o ambiente de geração não dispõe de Java 21/PostgreSQL e não conseguiu baixar as dependências Maven. Não há certificação de funcionamento integrado com dados reais nesta entrega.

## Arquitetura e contratos

| Camada | Responsabilidade |
|---|---|
| Next.js / React / TypeScript | Painéis responsivos, leitura/mapeamento de CSV, sessão e encaminhamento para a API |
| Spring Boot / Java | Autenticação, autorização, regras de contagem/tarefas e transação de importação |
| PostgreSQL / Flyway | Cadastros, saldos, atribuições, contagens, tarefas e histórico de importações |

Principais rotas:

- `GET /api/me`: identidade e perfil autenticados.
- `GET/POST /api/users`: consultar equipe e criar operadores (gestor).
- `GET/POST /api/imports`: histórico, prévia e confirmação (gestor). O POST recebe `requestKey`, `fileName`, `assignedUserId`, `rows` e `preview`.
- `GET/POST /api/inventory-counts`: consultar as permitidas e criar contagem manual (criação exclusiva do gestor).
- `PATCH /api/inventory-counts/{id}/assignment`: atribuição pelo gestor.
- `PATCH /api/inventory-counts/{id}/items/{itemId}` e `/{id}/finish`: registrar e finalizar.
- `GET/POST /api/tasks` e `PATCH /api/tasks/{id}`: distribuição e execução.
- `/api/products` e `/api/stock`: cadastro e saldo operacional existentes, exclusivos do gestor.

No navegador, as rotas de dados passam por `/api/data/...` do Next. Não há credencial de serviço compartilhada dando acesso de gestor ao operador.

## Limites atuais e próximas etapas

- Ainda não há leitura direta de XLSX/XML NF-e, exportação de ajustes para o ERP ou execução automática de ajustes após contagem.
- Lote, validade, avarias e recebimento têm tabelas reservadas na V1, mas ainda não possuem fluxo estruturado completo. As tarefas permitem registrar verificações em texto.
- Há cadastro de operador; não há edição/desativação de usuário, troca/recuperação de senha, MFA ou histórico detalhado de cada edição de contagem.
- Não há agendamento recorrente de tarefas, anexos, operação offline, leitura por câmera ou relatórios de produtividade.
- Uma instalação atende uma operação. Ainda não existe isolamento por empresa/tenant para vender uma instância compartilhada como SaaS.
- Para publicação: definir HTTPS, banco/API privados, backup e restauração, política de credenciais e proteção contra tentativas de login. Este patch não publica o site.

A documentação descreve os fluxos implementados e mantém explícitas as partes pendentes da visão maior do Vintra.

Referência da configuração externa do Spring: https://docs.spring.io/spring-boot/reference/features/external-config.html
