package com.openelements.cardless.data;

import java.util.List;

public record Requisition(String id, String created, String redirect, String status, String institutionId,
                          String agreement, String reference, List<String> accounts, String link) {
}
