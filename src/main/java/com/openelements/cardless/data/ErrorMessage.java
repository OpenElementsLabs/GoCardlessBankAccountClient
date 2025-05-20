package com.openelements.cardless.data;

public record ErrorMessage(String summary, String detail, int status_code) {
}
