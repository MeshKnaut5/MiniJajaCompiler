package fr.younesb.minijaja.error;

import java.util.Objects;

public record MjjError(int line, int column, ErrorCode code, String message) {

    public MjjError {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(message, "message must not be null");

        if (line < 1) {
            throw new IllegalArgumentException("line must be >= 1, got " + line);
        }
        if (column < 1) {
            throw new IllegalArgumentException("column must be >= 1, got " + column);
        }
        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be empty or blank");
        }
    }
}
