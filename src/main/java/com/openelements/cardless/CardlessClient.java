package com.openelements.cardless;

import com.openelements.cardless.data.Account;
import com.openelements.cardless.data.Balance;
import com.openelements.cardless.data.Institution;
import com.openelements.cardless.data.Requisition;
import com.openelements.cardless.data.RequisitionsPage;
import com.openelements.cardless.data.Transactions;
import com.openelements.cardless.internal.CardlessClientImpl;
import java.io.IOException;
import java.util.List;
import org.jspecify.annotations.NonNull;

public interface CardlessClient {

    static CardlessClient of(@NonNull final String secretId, @NonNull final String secretKey)
            throws CardlessException {
        return new CardlessClientImpl(secretId, secretKey);
    }

    @NonNull
    RequisitionsPage getRequisitions(final int limit, final int offset)
            throws CardlessException;

    @NonNull
    List<Institution> getInstitutions(@NonNull final String country) throws CardlessException;

    @NonNull
    Requisition createRequisition(@NonNull final Institution institution)
            throws CardlessException;

    @NonNull
    Requisition createRequisition(@NonNull final String institutionId) throws CardlessException;

    @NonNull
    Transactions getTransactions(@NonNull final String account) throws CardlessException;

    @NonNull
    Account getAccount(@NonNull final String id) throws CardlessException;

    @NonNull
    List<Balance> getBalances(@NonNull final String accountId) throws CardlessException;
}
