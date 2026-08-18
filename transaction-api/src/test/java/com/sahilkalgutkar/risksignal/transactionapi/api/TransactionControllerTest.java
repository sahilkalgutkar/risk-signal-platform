package com.sahilkalgutkar.risksignal.transactionapi.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sahilkalgutkar.risksignal.transactionapi.domain.Transaction;
import com.sahilkalgutkar.risksignal.transactionapi.domain.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @Test
    void submitReturns201WithLocationAndBody() throws Exception {
        Transaction transaction = new Transaction(
                "txn-1", "acct-1", new BigDecimal("50.00"), "USD", "US", "US", Instant.parse("2026-01-01T00:00:00Z"));
        when(transactionService.submit(any())).thenReturn(transaction);

        TransactionRequest request = new TransactionRequest("acct-1", new BigDecimal("50.00"), "USD", "US", "US");

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("txn-1"))
                .andExpect(jsonPath("$.accountId").value("acct-1"));
    }

    @Test
    void submitRejectsInvalidRequestWith400() throws Exception {
        TransactionRequest invalid = new TransactionRequest("", new BigDecimal("-5.00"), "us", "USA", "1x");

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReturns404WhenTransactionMissing() throws Exception {
        when(transactionService.findById(eq("missing"))).thenReturn(Optional.empty());

        mockMvc.perform(get("/transactions/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getReturnsTransactionWhenFound() throws Exception {
        Transaction transaction = new Transaction(
                "txn-1", "acct-1", new BigDecimal("50.00"), "USD", "US", "US", Instant.parse("2026-01-01T00:00:00Z"));
        when(transactionService.findById(eq("txn-1"))).thenReturn(Optional.of(transaction));

        mockMvc.perform(get("/transactions/txn-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("acct-1"));
    }
}
