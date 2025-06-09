package com.openelements.cardless.data;

import java.time.LocalDate;

public record BookedTransaction(String transactionId, String counterpartyName, CounterpartyAccount counterpartyAccount,
                                Amount transactionAmount, LocalDate bookingDate, LocalDate valueDate,
                                String remittanceInformationUnstructured, String additionalInformation) implements
        Transaction {
}
