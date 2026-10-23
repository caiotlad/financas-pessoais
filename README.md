# 1. API de finanças pessoais

API REST para registrar contas, categorias e movimentações e consultar um resumo mensal. O projeto usa Spring JDBC para deixar o SQL visível durante o estudo.

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
