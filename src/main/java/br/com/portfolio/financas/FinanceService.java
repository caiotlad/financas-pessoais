package br.com.portfolio.financas;

import static br.com.portfolio.financas.FinanceDtos.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
class FinanceService {
    private final JdbcTemplate jdbc;

    FinanceService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<AccountResponse> ACCOUNT = (rs, row) -> new AccountResponse(
            rs.getObject("id", UUID.class), rs.getString("name"), rs.getObject("created_at", OffsetDateTime.class));
    private static final RowMapper<CategoryResponse> CATEGORY = (rs, row) -> new CategoryResponse(
            rs.getObject("id", UUID.class), rs.getString("name"), Direction.valueOf(rs.getString("direction")));
    private static final RowMapper<TransactionResponse> TRANSACTION = (rs, row) -> new TransactionResponse(
            rs.getObject("id", UUID.class), rs.getObject("account_id", UUID.class),
            rs.getObject("category_id", UUID.class), Direction.valueOf(rs.getString("direction")),
            rs.getBigDecimal("amount"), rs.getString("description"),
            rs.getObject("occurred_at", OffsetDateTime.class));

    @Transactional
    AccountResponse createAccount(UUID ownerId, CreateAccountRequest request) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts(id, owner_id, name) VALUES (?, ?, ?)", id, ownerId, request.name().trim());
        return jdbc.queryForObject("SELECT id, name, created_at FROM accounts WHERE id = ?", ACCOUNT, id);
    }

    List<AccountResponse> listAccounts(UUID ownerId) {
        return jdbc.query("SELECT id, name, created_at FROM accounts WHERE owner_id = ? ORDER BY name", ACCOUNT, ownerId);
    }

    @Transactional
    CategoryResponse createCategory(UUID ownerId, CreateCategoryRequest request) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO categories(id, owner_id, name, direction) VALUES (?, ?, ?, ?)",
                id, ownerId, request.name().trim(), request.direction().name());
        return jdbc.queryForObject("SELECT id, name, direction FROM categories WHERE id = ?", CATEGORY, id);
    }

    List<CategoryResponse> listCategories(UUID ownerId) {
        return jdbc.query("SELECT id, name, direction FROM categories WHERE owner_id = ? ORDER BY direction, name", CATEGORY, ownerId);
    }

    @Transactional
    TransactionResponse createTransaction(UUID ownerId, CreateTransactionRequest request) {
        boolean accountExists = Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM accounts WHERE id = ? AND owner_id = ?)", Boolean.class,
                request.accountId(), ownerId));
        if (!accountExists) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada para este usuário");

        String categoryDirection = jdbc.query("SELECT direction FROM categories WHERE id = ? AND owner_id = ?",
                rs -> rs.next() ? rs.getString(1) : null, request.categoryId(), ownerId);
        if (categoryDirection == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada para este usuário");
        if (!categoryDirection.equals(request.direction().name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A direção da transação deve ser igual à da categoria");
        }

        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO ledger_transactions(id, owner_id, account_id, category_id, direction, amount, description, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, id, ownerId, request.accountId(), request.categoryId(), request.direction().name(),
                request.amount(), request.description().trim(), request.occurredAt());
        return jdbc.queryForObject("""
                SELECT id, account_id, category_id, direction, amount, description, occurred_at
                FROM ledger_transactions WHERE id = ?
                """, TRANSACTION, id);
    }

    List<TransactionResponse> listTransactions(UUID ownerId, OffsetDateTime from, OffsetDateTime to,
                                                UUID accountId, int limit, int offset) {
        if (limit < 1 || limit > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit deve estar entre 1 e 100");
        if (offset < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "offset não pode ser negativo");
        if (from != null && to != null && !from.isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from deve ser anterior a to");
        }
        return jdbc.query("""
                SELECT id, account_id, category_id, direction, amount, description, occurred_at
                FROM ledger_transactions
                WHERE owner_id = ?
                  AND (CAST(? AS timestamptz) IS NULL OR occurred_at >= ?)
                  AND (CAST(? AS timestamptz) IS NULL OR occurred_at < ?)
                  AND (CAST(? AS uuid) IS NULL OR account_id = ?)
                ORDER BY occurred_at DESC, id
                LIMIT ? OFFSET ?
                """, TRANSACTION, ownerId, from, from, to, to, accountId, accountId, limit, offset);
    }

    MonthlyReport monthlyReport(UUID ownerId, YearMonth month) {
        OffsetDateTime start = month.atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = month.plusMonths(1).atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        BigDecimal[] sums = jdbc.query("""
                SELECT COALESCE(SUM(amount) FILTER (WHERE direction = 'INCOME'), 0) AS income,
                       COALESCE(SUM(amount) FILTER (WHERE direction = 'EXPENSE'), 0) AS expenses
                FROM ledger_transactions WHERE owner_id = ? AND occurred_at >= ? AND occurred_at < ?
                """, rs -> {
                    if (!rs.next()) return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
                    return new BigDecimal[]{rs.getBigDecimal("income"), rs.getBigDecimal("expenses")};
                }, ownerId, start, end);
        List<CategoryTotal> categories = jdbc.query("""
                SELECT c.id AS category_id, c.name AS category_name, COALESCE(SUM(t.amount), 0) AS total
                FROM categories c JOIN ledger_transactions t ON t.category_id = c.id AND t.owner_id = c.owner_id
                WHERE c.owner_id = ? AND t.direction = 'EXPENSE' AND t.occurred_at >= ? AND t.occurred_at < ?
                GROUP BY c.id, c.name ORDER BY total DESC, c.name
                """, (rs, row) -> new CategoryTotal(rs.getObject("category_id", UUID.class),
                rs.getString("category_name"), rs.getBigDecimal("total")), ownerId, start, end);
        return new MonthlyReport(month.toString(), sums[0], sums[1], sums[0].subtract(sums[1]), categories);
    }
}
