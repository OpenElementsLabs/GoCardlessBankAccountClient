package com.openelements.cardless.data;

import java.time.LocalDate;

public record BookedTransaction(String transactionId, String debtorName, DebtorAccount debtorAccount,
                                Amount transactionAmount, LocalDate bookingDate, LocalDate valueDate,
                                String remittanceInformationUnstructured) implements Transaction {
}
