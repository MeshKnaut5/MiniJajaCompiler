package fr.younesb.minijaja.lexer;

import java.util.Objects;

public record Token(TokenType type, String lexeme, int line, int column) {
    public Token {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(lexeme, "lexeme must not be null");

        if (line < 1) {
            throw new IllegalArgumentException("line must be >= 1, got " + line);
        }
        if (column < 1) {
            throw new IllegalArgumentException("column must be >= 1, got " + column);
        }
        if (lexeme.isEmpty() && type != TokenType.EOF) {
            throw new IllegalArgumentException("lexeme must not be empty for non-EOF token " + type + " [line " + line + ", column " + column + "]");
        }
        if (type == TokenType.EOF && !lexeme.isEmpty()) {
            throw new IllegalArgumentException("EOF lexeme should be empty [line " + line + ", column " + column + "]");
        }
    }
}
