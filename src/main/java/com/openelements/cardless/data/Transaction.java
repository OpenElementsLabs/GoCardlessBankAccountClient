package com.openelements.cardless.data;

import java.time.LocalDate;

public interface Transaction {

    TransactionAmount transactionAmount();

    LocalDate valueDate();

    String remittanceInformationUnstructured();
}
