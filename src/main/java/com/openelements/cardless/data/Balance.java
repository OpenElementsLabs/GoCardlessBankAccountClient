package com.openelements.cardless.data;

import java.time.LocalDate;

public record Balance(Amount balanceAmount, String balanceType, LocalDate referenceDate) {
}
