package com.openelements.cardless;

import java.io.IOException;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

public class ApiCallException extends IOException {

    private final ErrorMessage errorMessage;

    public ApiCallException(@NonNull final ErrorMessage errorMessage) {
        super(Optional.ofNullable(errorMessage).map(com.openelements.cardless.ErrorMessage::detail).orElse(""));
        this.errorMessage = errorMessage;
    }

    public ErrorMessage getErrorMessage() {
        return errorMessage;
    }
}
