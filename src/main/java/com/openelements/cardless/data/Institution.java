package com.openelements.cardless.data;

public record Institution(String id, String name, String bic, String transactionTotalDays, String maxAccessValidForDays,
                          String logo) {
}
