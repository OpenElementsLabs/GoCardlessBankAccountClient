package com.openelements.cardless.data;

import java.time.LocalDate;

public record BookedTransaction(String transactionId, String debtorName, DebtorAccount debtorAccount,
                                TransactionAmount transactionAmount, LocalDate bookingDate, LocalDate valueDate,
                                String remittanceInformationUnstructured) implements Transaction {
}
