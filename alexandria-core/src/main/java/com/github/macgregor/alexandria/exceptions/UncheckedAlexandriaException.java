package com.github.macgregor.alexandria.exceptions;

public class UncheckedAlexandriaException extends RuntimeException {

    public UncheckedAlexandriaException(String message) {
        super(message);
    }

    public UncheckedAlexandriaException(String message, Throwable cause) {
        super(message, cause);
    }

    public UncheckedAlexandriaException(Throwable cause) {
        super(cause);
    }
}
