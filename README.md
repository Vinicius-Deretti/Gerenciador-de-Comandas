# Comandas API

API REST para um estabelecimento, em Java 17, Spring Boot 3.5.16, Maven, PostgreSQL, Spring Data JPA, Bean Validation, Flyway, MapStruct 1.6.3 e springdoc-openapi 2.8.17.

## Pré-requisitos

- JDK 17 ou superior compatível com o Spring Boot 3.5; `java` disponível no PATH.
- Docker e Docker Compose com suporte a containers Linux para iniciar o banco e executar Testcontainers.
- Acesso ao Maven Central na primeira execução. O Maven Wrapper oficial instala o Maven 3.9.16; não é necessário instalar Maven.

## Iniciar o PostgreSQL

Copie `.env.example` para `.env` e substitua a senha de exemplo por uma senha local. Esse arquivo está ignorado pelo Git.

PowerShell:

```powershell
Copy-Item .env.example .env
# Edite .env antes de continuar.
docker compose up -d --wait
docker compose ps
```

Linux/macOS: `cp .env.example .env`, edite o arquivo e execute os mesmos comandos Docker.

O PostgreSQL 17 fica exposto somente em `127.0.0.1:5432`, com dados no volume `postgres_data`. Para parar sem perder os dados: `docker compose down`.

## Variáveis de ambiente

| Variável | Padrão / finalidade |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/comandas` |
| `DB_USERNAME` | `comandas` |
| `DB_PASSWORD` | Obrigatória, sem valor padrão na API |
| `DB_NAME` | `comandas`; nome do banco criado pelo Compose |
| `DB_PORT` | `5432`; porta publicada pelo Compose |
| `ESTABELECIMENTO_FUSO` | `America/Sao_Paulo`; identificador IANA, por exemplo `America/Manaus` |
| `SERVER_PORT` | `8080` |

O Compose lê `.env` automaticamente. A aplicação Java lê as variáveis do processo; carregue-as no terminal antes de iniciá-la. Ao mudar `DB_PORT` ou `DB_NAME`, ajuste também `DB_URL`.

PowerShell, para o formato simples `CHAVE=valor` de `.env.example`:

```powershell
Get-Content .env | Where-Object { $_ -match '^[A-Z_]+=' } | ForEach-Object {
    $par = $_ -split '=', 2
    [Environment]::SetEnvironmentVariable($par[0], $par[1], 'Process')
}
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```sh
set -a
. ./.env
set +a
sh mvnw spring-boot:run
```

Para gerar e executar o JAR:

```powershell
.\mvnw.cmd package -DskipTests
java -jar target/comandas-api-0.0.1-SNAPSHOT.jar
```

Use `sh mvnw` no lugar de `.\mvnw.cmd` em Linux/macOS. O Flyway aplica as migrations na inicialização. O Hibernate apenas valida o schema (`ddl-auto=validate`).

## Swagger

Com a API iniciada:

- Interface: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Os endpoints têm descrições, schemas com validações e exemplos. Substitua os UUIDs ilustrativos pelos retornados na criação.

## Endpoints

Todos os caminhos abaixo começam com `/api/v1`.

| Método | Caminho | Operação |
| --- | --- | --- |
| POST / GET | `/categorias` | Criar / listar |
| GET / PUT / DELETE | `/categorias/{id}` | Consultar / editar / desativar |
| POST / GET | `/produtos` | Criar / listar |
| GET / PUT / DELETE | `/produtos/{id}` | Consultar / editar / desativar |
| PUT | `/produtos/{id}/variacoes` | Atualizar coleção de variações |
| POST / GET | `/comandas` | Abrir / listar abertas |
| GET | `/comandas/{id}` | Detalhe, itens, total e pagamento |
| POST | `/comandas/{id}/itens` | Incluir item |
| GET / PATCH / DELETE | `/comandas/{id}/itens/{itemId}` | Consultar / alterar quantidade / remover |
| POST | `/comandas/{id}/finalizacao` | Finalizar e receber pagamento |
| GET | `/comandas/historico` | Listar finalizadas |
| GET | `/dashboard/diario?data=2026-10-26` | Receita do dia |
| GET | `/dashboard/semanal?data=2026-10-26` | Receita da semana da data |
| GET | `/dashboard/mensal?data=2026-10-26` | Receita do mês da data |
| GET | `/dashboard/periodo?inicio=2026-10-01&fim=2026-10-31&agrupamento=DIA` | Resumo e grupos do intervalo |

Listas usam `page` (base zero, padrão 0) e `size` (1 a 100, padrão 20), com ordenação fixa e desempate por ID. O formato é `{content, number, size, totalElements, totalPages}`. Categorias e produtos filtram por `ativo` (padrão `true`); produtos aceitam `categoriaId` opcional. Consultas por ID incluem recursos inativos e todas as variações, com seus respectivos estados.

O histórico aceita `inicio` e `fim` opcionais, informados juntos, sobre a data de finalização. Sem filtro, retorna todo o histórico paginado. Comandas finalizadas continuam consultáveis e deixam a listagem inicial. Não há exclusão de comandas.

## Exemplo de fluxo

Requisições JSON para `http://localhost:8080`, com `Content-Type: application/json`.

1. **Cadastrar categoria**: `POST /api/v1/categorias`

```json
{"nome":"Salgados"}
```

Guarde o `id` retornado como `categoriaId`.

2. **Cadastrar produto com um preço**: `POST /api/v1/produtos`

```json
{
  "nome": "Coxinha",
  "categoriaId": "UUID_DA_CATEGORIA",
  "variacoes": [{"preco": 7.00}]
}
```

O nome da única variação pode ser nulo. Guarde o ID do produto e `variacoes[0].id`.

3. **Adicionar uma segunda opção e nomear a primeira na mesma transação**: `PUT /api/v1/produtos/{id}/variacoes`

```json
{
  "variacoes": [
    {"id":"UUID_DA_PRIMEIRA_VARIACAO","nome":"Pequena","preco":7.00},
    {"nome":"Grande","preco":12.00}
  ]
}
```

Envie a coleção completa de opções que devem ficar ativas. IDs existentes são preservados; opções sem ID recebem novos IDs; opções omitidas são desativadas. IDs repetidos ou pertencentes a outro produto causam 409. Incluir um ID inativo do mesmo produto o reativa.

Duas ou mais opções ativas exigem nomes em todas. Ao voltar a uma opção, seu nome pode ser mantido ou omitido. Uma coleção vazia só é permitida em produto inativo. Na criação, sempre se exige pelo menos uma opção.

`PUT /produtos/{id}` atualiza os dados gerais com `nome`, `categoriaId`, `descricao`, `imagemUrl` e `ativo`; categoria, nome e ativo são obrigatórios. As variações são alteradas pelo endpoint próprio. Após um DELETE do produto, suas variações também ficam inativas; antes de reativá-lo, ative pelo menos uma variação pela operação de coleção.

4. **Abrir comanda**: `POST /api/v1/comandas`

```json
{"nomeCliente":"Maria"}
```

5. **Incluir item**: `POST /api/v1/comandas/{id}/itens`

```json
{"variacaoId":"UUID_DA_VARIACAO","quantidade":2}
```

Cada inclusão cria um item com os nomes e o preço daquele momento. O retorno contém a comanda atualizada e o cabeçalho `Location` identifica o item criado. Para alterar sua quantidade: `PATCH /comandas/{id}/itens/{itemId}` com `{"quantidade":3}`. O preço histórico é preservado, mesmo após edição ou desativação do catálogo.

Campos desconhecidos no JSON são rejeitados. O aplicativo informa somente variação e quantidade; preço, subtotal e total são calculados no servidor. Quantidades fracionárias e preços com mais de duas casas decimais (inclusive `7.000`) retornam 400. Valores persistidos têm limite de `9999999999.99`; cálculos que excedem esse limite retornam 409, sem alteração parcial.

6. **Finalizar**: `POST /api/v1/comandas/{id}/finalizacao`, sem corpo.

O retorno inclui status `FINALIZADA`, data de finalização e pagamento integral no total calculado. Não se informa valor entregue, troco ou forma de pagamento nesta versão.

7. Consulte `GET /api/v1/comandas/historico` e `GET /api/v1/dashboard/diario`.

## Datas, receita e agrupamentos

Instantes são persistidos em `TIMESTAMPTZ` e retornados em UTC (`Z`), com JDBC e sessões do banco configurados em UTC. A data local do estabelecimento usa `ESTABELECIMENTO_FUSO`, independente do fuso do servidor.

Filtros `inicio` e `fim` são datas inclusivas. Internamente a consulta usa `[inicio às 00:00, dia seguinte a fim às 00:00)`, convertidos pelo fuso, respeitando inclusive regras históricas de horário de verão.

- Diário: um dia local.
- Semanal: segunda-feira até domingo, inclusive, da semana da data de referência.
- Mensal: primeiro ao último dia do mês da data de referência.
- `data` omitida: data atual do estabelecimento.
- Intervalo: `inicio` e `fim` obrigatórios; `agrupamento=DIA|SEMANA|MES`.

Receita é a soma de `pagamento.valor_pago` filtrada por `pagamento.recebido_em`. A quantidade é a contagem desses pagamentos, já que cada comanda finalizada tem exatamente um. Ticket médio é receita / quantidade, com duas casas decimais e HALF_UP apenas na apresentação da média; sem recebimentos, retorna 0.00.

Os grupos são calculados no PostgreSQL, paginados e ordenados pelo início do período. Grupos sem recebimentos são omitidos. O campo `periodo` é a data inicial do grupo; em semanas é a segunda-feira, inclusive na virada de ano. Grupos nas bordas incluem somente os recebimentos dentro do intervalo solicitado. Totais consideram todo o filtro, independentemente da página. Não há tabela de dashboard. A transação de leitura usa REPEATABLE READ para manter resumo, contagem e grupos na mesma visão dos dados.

## Integridade e concorrência

Todos os comandos que alteram itens ou finalizam uma comanda obtêm `PESSIMISTIC_WRITE` na linha da comanda (`SELECT ... FOR UPDATE`). Assim, duas finalizações ou uma edição concorrente com finalização são serializadas no banco, inclusive entre instâncias da API.

A transação verifica o status e os itens, recalcula o total pelos preços históricos, insere o pagamento e atualiza status/data. Qualquer falha reverte tudo. A segunda finalização retorna **409**, sem criar pagamento. A restrição `UNIQUE (comanda_id)` em pagamento é uma segunda proteção.

Alterações de catálogo também usam bloqueios: produto antes da categoria, quando ambos são necessários. Novos lançamentos seguem a mesma ordem após bloquear a comanda, garantindo uma leitura consistente do preço e dos estados ativos. A criação de produto e a desativação de categoria bloqueiam a categoria, evitando novos produtos em uma categoria desativada.

As chaves estrangeiras não têm exclusão em cascata. JPA não usa `CascadeType.REMOVE` ou `orphanRemoval`. Categorias, produtos e variações são desativados; itens só são removidos explicitamente de comandas abertas. A desativação de categoria é impedida se houver produto ativo, inclusive ao editar `ativo=false`.

## Erros

Criações retornam 201; consultas e alterações com corpo, 200; exclusões lógicas e remoção de item, 204. Entradas inválidas retornam 400; recursos inexistentes, 404; conflitos e regras de negócio, 409.

```json
{
  "timestamp": "2026-10-26T15:00:00Z",
  "status": 400,
  "mensagem": "Existem campos inválidos.",
  "path": "/api/v1/categorias",
  "campos": [{"campo":"nome","mensagem":"não deve estar em branco"}]
}
```

## Executar os testes

Somente testes unitários:

```powershell
.\mvnw.cmd test
```

Verificação completa, com integração, migrations e geração do JAR:

```powershell
.\mvnw.cmd verify
```

Por padrão, os testes de integração (`*IT`) iniciam um PostgreSQL 17.6 com Testcontainers; o Docker precisa estar ativo. Falta de Docker causa falha explícita. Não há H2 nem substituição de banco.

Alternativamente, use um PostgreSQL dedicado a testes:

```powershell
$env:TEST_DB_URL = 'jdbc:postgresql://localhost:5432/comandas_test'
$env:TEST_DB_USERNAME = 'comandas'
$env:TEST_DB_PASSWORD = 'SENHA_DO_BANCO_DE_TESTES'
.\mvnw.cmd verify
```

**Use um banco descartável e exclusivo para testes**: a suíte limpa as seis tabelas entre cenários e cria temporariamente um trigger para simular falha de pagamento. O banco precisa existir e o usuário deve poder aplicar migrations e criar funções/triggers. Não execute as classes de integração em paralelo no mesmo banco.

Os testes cobrem variações e seus IDs, exclusão lógica, entradas inválidas, preços históricos, imutabilidade de comandas finalizadas, rollback, finalizações simultâneas, edição concorrente, receita por pagamentos, paginação, virada de semana/ano, fronteiras dos períodos, fuso configurável, Swagger e horário de verão histórico.

Validação realizada na implementação: **6 testes unitários e 17 de integração aprovados**, com Maven Wrapper e PostgreSQL 17.6 real. Flyway e validação do schema pelo Hibernate executados com sucesso. Como o serviço Docker não estava acessível no ambiente, a integração foi executada por `TEST_DB_URL` em uma instância PostgreSQL temporária; a inicialização via Compose/Testcontainers não foi verificada neste ambiente.

A configuração do Compose passou em `docker compose config --quiet`. O ambiente restrito negou o encerramento da instância temporária e a limpeza de seus arquivos, todos sob `.build/` (ignorado pelo Git). A instância de teste escuta somente em `127.0.0.1:55432` e pode ser encerrada em um PowerShell local, a partir da raiz do projeto:

```powershell
& .\.build\pg\pgsql\bin\pg_ctl.exe -D .build/pgdata -m fast -w stop
```

## Estrutura e escopo

Código em `src/main/java/br/com/comandas`, organizado em `categoria`, `produto`, `comanda`, `pagamento`, `dashboard`, `shared/exception` e `shared/config`. Controllers validam entrada e retornam DTOs; services aplicam regras/transações; repositories consultam o banco; mappers MapStruct geram a conversão na compilação.

Sem autenticação, entidade Cliente, aplicativo mobile, upload, SQLite no backend, sincronização offline, pagamento parcial, descontos, taxas ou divisão de pagamento. A API recebe somente URLs HTTP/HTTPS de imagens.
