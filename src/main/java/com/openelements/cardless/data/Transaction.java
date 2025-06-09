package com.openelements.cardless.data;

import java.time.LocalDate;

public interface Transaction {

    Amount transactionAmount();

    LocalDate valueDate();

    String remittanceInformationUnstructured();

    String additionalInformation();
}
