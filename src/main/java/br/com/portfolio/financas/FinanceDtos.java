package br.com.portfolio.financas;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

final class FinanceDtos {
    private FinanceDtos() {}

    record CreateAccountRequest(@NotBlank @Size(max = 100) String name) {}
    record AccountResponse(UUID id, String name, OffsetDateTime createdAt) {}
    record CreateCategoryRequest(@NotBlank @Size(max = 80) String name, @NotNull Direction direction) {}
    record CategoryResponse(UUID id, String name, Direction direction) {}
    record CreateTransactionRequest(
            @NotNull UUID accountId,
            @NotNull UUID categoryId,
            @NotNull Direction direction,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
            @NotBlank @Size(max = 240) String description,
            @NotNull OffsetDateTime occurredAt) {}
    record TransactionResponse(UUID id, UUID accountId, UUID categoryId, Direction direction,
                              BigDecimal amount, String description, OffsetDateTime occurredAt) {}
    record CategoryTotal(UUID categoryId, String categoryName, BigDecimal total) {}
    record MonthlyReport(String month, BigDecimal income, BigDecimal expenses,
                         BigDecimal balance, List<CategoryTotal> expensesByCategory) {}
    enum Direction { INCOME, EXPENSE }
}
