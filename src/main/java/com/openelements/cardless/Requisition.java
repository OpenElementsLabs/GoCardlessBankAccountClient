package com.openelements.cardless;

public record Requisition(String id, String created, String redirect, String status, String institution_id,
                          String agreement, String reference, String link) {
}
