package fr.younesb.minijaja.lexer;

import fr.younesb.minijaja.error.MjjError;
import java.util.List;
import java.util.Objects;

public record LexResult(List<Token> tokens, List<MjjError> errors) {

    public LexResult {
        Objects.requireNonNull(tokens, "tokens list must not be null");
        Objects.requireNonNull(errors, "errors list must not be null");

        tokens = List.copyOf(tokens);
        errors = List.copyOf(errors);

        if (tokens.isEmpty()) throw new IllegalArgumentException("tokens list must not be empty");

        if (tokens.getLast().type() != TokenType.EOF) {
            throw new IllegalArgumentException(
                    "last token is of type " + tokens.getLast().type() + ", must be of type EOF");
        }
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
