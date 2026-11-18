package ru.platonso.finance.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Transaction(
    TransactionType type, String category, BigDecimal amount, LocalDate date) {}
