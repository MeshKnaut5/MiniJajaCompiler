package fr.younesb.minijaja.lexer;

import fr.younesb.minijaja.error.ErrorCode;
import fr.younesb.minijaja.error.MjjError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Lexer {
    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
            Map.entry("class", TokenType.CLASS),
            Map.entry("final", TokenType.FINAL),
            Map.entry("main", TokenType.MAIN),
            Map.entry("void", TokenType.VOID),
            Map.entry("int", TokenType.INT),
            Map.entry("boolean", TokenType.BOOLEAN),
            Map.entry("if", TokenType.IF),
            Map.entry("else", TokenType.ELSE),
            Map.entry("while", TokenType.WHILE),
            Map.entry("return", TokenType.RETURN),
            Map.entry("true", TokenType.TRUE),
            Map.entry("false", TokenType.FALSE),
            Map.entry("length", TokenType.LENGTH),
            Map.entry("write", TokenType.WRITE),
            Map.entry("writeln", TokenType.WRITELN)
    );

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private final List<MjjError> errors = new ArrayList<>();
    private int current = 0;
    private int start = 0;
    private int column = 1;
    private int line = 1;
    private int startLine;
    private int startColumn;

    private Lexer(String source) {
        Objects.requireNonNull(source, "source must not be null");
        this.source = source;
    }

    // Public entry point
    public static LexResult scan(String source) {
        Lexer lexer = new Lexer(source);
        lexer.scanTokens();
        return new LexResult(lexer.tokens, lexer.errors);
    }

    // MAIN SCANNING LOOP
    private void scanTokens() {
        while (!isAtEnd()) {
            start = current;
            startLine = line;
            startColumn = column;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", line, column));
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(' -> addToken(TokenType.LEFT_PAR);
            case ')' -> addToken(TokenType.RIGHT_PAR);
            case '[' -> addToken(TokenType.LEFT_BRACK);
            case ']' -> addToken(TokenType.RIGHT_BRACK);
            case '{' -> addToken(TokenType.LEFT_BRACE);
            case '}' -> addToken(TokenType.RIGHT_BRACE);
            case ';' -> addToken(TokenType.SEMICOL);
            case ',' -> addToken(TokenType.COMMA);
            case '-' -> addToken(TokenType.MINUS);
            case '*' -> addToken(TokenType.STAR);
            case '>' -> addToken(TokenType.GREATER);
            case '!' -> addToken(TokenType.BANG);
            case '\t', '\n', ' ' -> {}
            case '=' -> addToken(match('=') ? TokenType.EQ_EQ : TokenType.EQ);
            case '/' -> {
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else if (match('*')) {
                    scanBlockComment();
                } else {
                    addToken(TokenType.SLASH);
                }
            }
            case '"' -> scanString();
            case '+' -> {
                if (match('+')) {
                    addToken(TokenType.PLUS_PLUS);
                } else if (match('=')) {
                    addToken(TokenType.PLUS_EQ);
                } else {
                    addToken(TokenType.PLUS);
                }
            }
            case '&' -> {
                if (match('&')) {
                    addToken(TokenType.AMPER_AMPER);
                } else {
                    addError(ErrorCode.LONE_AMPER, "lone & (did you mean && ?)");
                }
            }
            case '|' -> {
                if (match('|')) {
                    addToken(TokenType.PIPE_PIPE);
                } else {
                    addError(ErrorCode.LONE_PIPE, "lone | (did you mean || ?)");
                }
            }
            default -> {
                if (isDigit(c)) {
                    scanNumber();
                } else if (isAlpha(c)) {
                    scanIdentifier();
                } else {
                    addError(ErrorCode.UNEXPECTED_CHARACTER,
                            "unexpected character '" + source.substring(start, current) + "'");
                }
            }
        }
    }

    private void scanString() {
        while (peek() != '"' && !isAtEnd()) {
            advance();
        }

        if (isAtEnd()) {
            addError(ErrorCode.UNTERMINATED_STRING, "missing '\"', unterminated string");
            return;
        }
        advance();

        addToken(TokenType.STRING_LITERAL);
    }

    private void scanBlockComment() {
        while (!(peek() == '*' && peekNext() == '/')) {
            if (isAtEnd()) {
                addError(ErrorCode.UNTERMINATED_BLOCK_COMMENT,
                        "missing '/', unterminated block comment");
                return;
            }
            advance();
        }
        advance();
        advance();
    }

    private void scanIdentifier() {
        while (isAlphaNumeric(peek())) advance();
        addToken(KEYWORDS.getOrDefault(source.substring(start, current), TokenType.IDENT));
    }

    private void scanNumber() {
        while (isDigit(peek())) advance();

        addToken(TokenType.INT_LITERAL);
    }

    // HELPER
    private void addError(ErrorCode code, String message) {
        errors.add(new MjjError(startLine, startColumn, code, message));
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        char c = source.charAt(current++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private char peek() {
        if (!isAtEnd()) {
            return source.charAt(current);
        } else {
            return '\0';
        }
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        advance();
        return true;
    }

    private void addToken(TokenType type) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, startLine,startColumn));
    }
}
