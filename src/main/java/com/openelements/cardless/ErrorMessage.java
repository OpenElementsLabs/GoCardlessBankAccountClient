package com.openelements.cardless;

public record ErrorMessage(String summary, String detail, int status_code) {
}
