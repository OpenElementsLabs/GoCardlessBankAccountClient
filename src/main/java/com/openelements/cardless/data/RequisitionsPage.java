package com.openelements.cardless.data;

import java.util.List;

public record RequisitionsPage(int count, String next, String previous, List<Requisition> requisitions) {
}
