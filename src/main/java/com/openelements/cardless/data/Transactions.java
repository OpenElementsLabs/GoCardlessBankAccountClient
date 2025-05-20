package com.openelements.cardless.data;

import java.util.List;

public record Transactions(List<BookedTransaction> bookedTransactions, List<PendingTransaction> pendingTransactions) {

}
