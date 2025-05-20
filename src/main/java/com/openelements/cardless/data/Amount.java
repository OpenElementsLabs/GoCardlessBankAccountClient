package com.openelements.cardless.data;

import java.math.BigDecimal;

public record Amount(String currency, BigDecimal amount) {
}
