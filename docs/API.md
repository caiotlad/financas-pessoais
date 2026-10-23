# API de finanças pessoais

Base local: `http://localhost:8080`. Envie `X-Owner-Id: <UUID>` em todas as rotas. Use o mesmo UUID para os dados que devem pertencer à mesma pessoa.

## 1. Criar uma conta

```bash
curl -i -X POST http://localhost:8080/api/accounts \
  -H 'Content-Type: application/json' \
  -H 'X-Owner-Id: 11111111-1111-4111-8111-111111111111' \
  -d '{"name":"Conta principal"}'
```

Resposta `201 Created`, contendo o UUID da conta.

## 2. Criar categorias

```bash
curl -i -X POST http://localhost:8080/api/categories \
  -H 'Content-Type: application/json' \
  -H 'X-Owner-Id: 11111111-1111-4111-8111-111111111111' \
  -d '{"name":"Salário","direction":"INCOME"}'

curl -i -X POST http://localhost:8080/api/categories \
  -H 'Content-Type: application/json' \
  -H 'X-Owner-Id: 11111111-1111-4111-8111-111111111111' \
  -d '{"name":"Moradia","direction":"EXPENSE"}'
```

Guarde o `id` retornado para usar nas transações. Os valores aceitos para `direction` são `INCOME` e `EXPENSE`.

## 3. Registrar uma transação

Substitua os dois UUIDs do JSON pelos IDs retornados ao criar a conta e a categoria:

```bash
curl -i -X POST http://localhost:8080/api/transactions \
  -H 'Content-Type: application/json' \
  -H 'X-Owner-Id: 11111111-1111-4111-8111-111111111111' \
  -d '{
    "accountId":"<ID_DA_CONTA>",
    "categoryId":"<ID_DA_CATEGORIA>",
    "direction":"EXPENSE",
    "amount":1250.50,
    "description":"Aluguel de outubro",
    "occurredAt":"2026-10-05T12:00:00Z"
  }'
```

A direção da transação precisa combinar com a direção da categoria. O valor é positivo; `direction` determina se será somado como receita ou despesa.

## 4. Consultar dados

```bash
curl -H 'X-Owner-Id: 11111111-1111-4111-8111-111111111111' \
  'http://localhost:8080/api/accounts'

curl -H 'X-Owner-Id: 11111111-1111-4111-8111-111111111111' \
  'http://localhost:8080/api/transactions?from=2026-10-01T00:00:00Z&to=2026-11-01T00:00:00Z&limit=20&offset=0'

curl -H 'X-Owner-Id: 11111111-1111-4111-8111-111111111111' \
  'http://localhost:8080/api/reports/monthly?month=2026-10'
```

`from` é inclusivo e `to` é exclusivo. `limit` aceita 1–100 e `offset` deve ser zero ou maior. O relatório devolve receita, despesa, saldo e totais de despesa por categoria.

## Respostas de erro

- `400 Bad Request`: corpo inválido, datas em ordem errada ou limite de paginação fora da faixa.
- `404 Not Found`: conta ou categoria não pertence ao UUID informado.
- `409 Conflict`: nome de conta ou categoria duplicado para o mesmo proprietário.
