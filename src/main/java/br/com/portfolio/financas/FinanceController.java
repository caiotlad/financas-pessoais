package br.com.portfolio.financas;

import static br.com.portfolio.financas.FinanceDtos.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
class FinanceController {
    private final FinanceService service;

    FinanceController(FinanceService service) {
        this.service = service;
    }

    @PostMapping("/accounts")
    ResponseEntity<AccountResponse> createAccount(@RequestHeader("X-Owner-Id") UUID ownerId,
                                                  @Valid @RequestBody CreateAccountRequest request) {
        AccountResponse created = service.createAccount(ownerId, request);
        return ResponseEntity.created(URI.create("/api/accounts/" + created.id())).body(created);
    }

    @GetMapping("/accounts")
    List<AccountResponse> listAccounts(@RequestHeader("X-Owner-Id") UUID ownerId) {
        return service.listAccounts(ownerId);
    }

    @PostMapping("/categories")
    ResponseEntity<CategoryResponse> createCategory(@RequestHeader("X-Owner-Id") UUID ownerId,
                                                    @Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse created = service.createCategory(ownerId, request);
        return ResponseEntity.created(URI.create("/api/categories/" + created.id())).body(created);
    }

    @GetMapping("/categories")
    List<CategoryResponse> listCategories(@RequestHeader("X-Owner-Id") UUID ownerId) {
        return service.listCategories(ownerId);
    }

    @PostMapping("/transactions")
    ResponseEntity<TransactionResponse> createTransaction(@RequestHeader("X-Owner-Id") UUID ownerId,
                                                          @Valid @RequestBody CreateTransactionRequest request) {
        TransactionResponse created = service.createTransaction(ownerId, request);
        return ResponseEntity.created(URI.create("/api/transactions/" + created.id())).body(created);
    }

    @GetMapping("/transactions")
    List<TransactionResponse> listTransactions(
            @RequestHeader("X-Owner-Id") UUID ownerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return service.listTransactions(ownerId, from, to, accountId, limit, offset);
    }

    @GetMapping("/reports/monthly")
    MonthlyReport monthlyReport(@RequestHeader("X-Owner-Id") UUID ownerId,
                                @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return service.monthlyReport(ownerId, month);
    }
}
