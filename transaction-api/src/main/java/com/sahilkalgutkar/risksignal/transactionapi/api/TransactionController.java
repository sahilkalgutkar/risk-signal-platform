package com.sahilkalgutkar.risksignal.transactionapi.api;

import com.sahilkalgutkar.risksignal.transactionapi.domain.Transaction;
import com.sahilkalgutkar.risksignal.transactionapi.domain.TransactionNotFoundException;
import com.sahilkalgutkar.risksignal.transactionapi.domain.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> submit(@Valid @RequestBody TransactionRequest request) {
        Transaction transaction = transactionService.submit(request);
        TransactionResponse body = TransactionResponse.from(transaction);
        return ResponseEntity.created(URI.create("/transactions/" + transaction.getId())).body(body);
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable String id) {
        return transactionService.findById(id)
                .map(TransactionResponse::from)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(TransactionNotFoundException e) {
        return new ErrorResponse(e.getMessage());
    }

    public record ErrorResponse(String message) {
    }
}
