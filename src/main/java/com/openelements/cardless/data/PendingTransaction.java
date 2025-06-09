package com.openelements.cardless.data;

import java.time.LocalDate;

public record PendingTransaction(Amount transactionAmount, LocalDate valueDate,
                                 String remittanceInformationUnstructured, String additionalInformation) implements
        Transaction {
}
