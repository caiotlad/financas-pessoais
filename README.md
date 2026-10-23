# 1. API de finanças pessoais

API REST para registrar contas, categorias e movimentações e consultar um resumo mensal. O projeto usa Spring JDBC para deixar o SQL visível durante o estudo.

**Repositório GitHub:** [`caiotlad/financas-pessoais`](https://github.com/caiotlad/financas-pessoais) (já criado).

## Requisitos

- Java 17 ou superior
- Maven 3.6.3 ou superior
- Docker com Docker Compose

## Executar localmente

```bash
docker compose up -d
mvn spring-boot:run
```

A API fica em `http://localhost:8080`; o PostgreSQL fica na porta `5433`. Na primeira inicialização, o Flyway cria as tabelas e índices definidos em `src/main/resources/db/migration/V1__schema_inicial.sql`.

Para encerrar o banco: `docker compose down`. Os dados continuam no volume; para apagá-los também: `docker compose down -v`.

Leia `docs/GUIA_DE_ESTUDO.md` e `docs/API.md` antes de percorrer o código.

## Funcionalidades do MVP

- Criar e listar contas.
- Criar e listar categorias de receita e despesa.
- Registrar transações associadas a uma conta e a uma categoria do mesmo proprietário.
- Listar transações por período e conta, com paginação por `limit` e `offset`.
- Somar receitas, despesas e saldo do mês, além de agrupar despesas por categoria.

## Decisões importantes

- `BigDecimal` e `NUMERIC(14,2)` representam valores monetários; `double` não é usado.
- O MVP assume uma moeda única (BRL) e não converte valores entre moedas.
- `X-Owner-Id` separa os dados de demonstração por UUID. Ele **não autentica** a pessoa que envia a requisição.
- Chaves estrangeiras compostas garantem que uma transação não associe a conta ou categoria de outro proprietário.
- O relatório usa intervalo semiaberto `[início do mês, início do próximo mês)`, que funciona bem com horários e índices.

Este é um projeto didático para execução local. Não cadastre dados financeiros reais: autenticação, autorização, edição/exclusão, recorrência e orçamento ainda não foram implementados.

## Organização de pastas

```text
01-financas-pessoais/
├── docs/
│   ├── API.md                 # rotas e exemplos curl
│   └── GUIA_DE_ESTUDO.md      # modelo, SQL e conceitos
├── src/main/java/br/com/portfolio/financas/
│   ├── FinancasApplication.java
│   ├── FinanceController.java # endpoints HTTP
│   ├── FinanceDtos.java       # contratos e validação
│   ├── FinanceService.java    # regras de negócio e SQL
│   └── ApiExceptionHandler.java
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/V1__schema_inicial.sql
├── compose.yaml
├── pom.xml
└── README.md
```

## Modelo de dados e fluxo

`accounts` e `categories` pertencem a um `owner_id`. `ledger_transactions` liga os dois e registra direção, valor, descrição e instante. As chaves estrangeiras compostas fazem o PostgreSQL validar que conta e categoria pertencem ao mesmo proprietário da transação.

Ao criar uma transação, o controller valida o JSON, o serviço confere a conta, a categoria e a compatibilidade entre direção e categoria, e então insere o lançamento em uma transação SQL. O saldo é calculado a partir do histórico, sem uma coluna de saldo que possa ficar dessincronizada.

## Endpoints

Todas as rotas abaixo exigem `X-Owner-Id: <UUID>`.

| Método | Rota | Responsabilidade |
|---|---|---|
| `POST` | `/api/accounts` | Criar conta |
| `GET` | `/api/accounts` | Listar contas do proprietário |
| `POST` | `/api/categories` | Criar categoria de receita/despesa |
| `GET` | `/api/categories` | Listar categorias |
| `POST` | `/api/transactions` | Validar relações e registrar lançamento |
| `GET` | `/api/transactions` | Filtrar por período/conta com paginação |
| `GET` | `/api/reports/monthly?month=AAAA-MM` | Agregar totais do mês |

Os payloads completos estão em [`docs/API.md`](docs/API.md). Para entender a ordem de leitura do código, as chaves e os índices, consulte [`docs/GUIA_DE_ESTUDO.md`](docs/GUIA_DE_ESTUDO.md).

## Como estudar este projeto

1. Leia a migração SQL e identifique cada chave/`CHECK`.
2. Siga uma rota em `FinanceController` até a consulta correspondente em `FinanceService`.
3. Compare as validações Java com as restrições do banco.
4. Execute o relatório mensal para entender `SUM`, `FILTER`, `GROUP BY` e o intervalo de datas.
5. Como exercício, substitua `X-Owner-Id` por uma identidade autenticada antes de qualquer uso além do ambiente local.
