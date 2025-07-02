package com.openelements.cardless;

import org.jspecify.annotations.NonNull;

public class CardlessException extends Exception {
    public CardlessException(@NonNull final String message) {
        super(message);
    }

    public CardlessException(@NonNull final String message, final Throwable cause) {
        super(message, cause);
    }
}
