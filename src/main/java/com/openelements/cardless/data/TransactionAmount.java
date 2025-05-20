package com.openelements.cardless.data;

import java.math.BigDecimal;

public record TransactionAmount(String currency, BigDecimal amount) {
}
