package com.openelements.cardless.data;

import java.time.LocalDate;

public record PendingTransaction(TransactionAmount transactionAmount, LocalDate valueDate,
                                 String remittanceInformationUnstructured) implements Transaction {
}
