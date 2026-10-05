package fr.younesb.minijaja.lexer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TokenTest {
    @Test
    void constructor_validIdent_createsToken() {
        Token t = assertDoesNotThrow(() -> new Token(TokenType.IDENT, "x", 1, 1));

        assertEquals(TokenType.IDENT, t.type());
        assertEquals("x", t.lexeme());
        assertEquals(1, t.line());
        assertEquals(1, t.column());
    }

    @Test
    void constructor_eofWithEmptyLexeme_createsToken() {
        assertDoesNotThrow(() -> new Token(TokenType.EOF, "", 3, 7));
    }

    @Test
    void constructor_nullType_throwsNullPointerException() {
        NullPointerException e = assertThrows(NullPointerException.class,
                () -> new Token(null, "x", 1, 1));

        assertEquals("type must not be null", e.getMessage());
    }

    @Test
    void constructor_nullLexeme_throwsNullPointerException() {
        NullPointerException e = assertThrows(NullPointerException.class,
                () -> new Token(TokenType.IDENT, null, 1, 1));

        assertEquals("lexeme must not be null", e.getMessage());
    }

    @Test
    void constructor_lineZero_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Token(TokenType.IDENT, "x", 0, 1));
    }

    @Test
    void constructor_columnZero_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Token(TokenType.IDENT, "x", 1, 0));
    }

    @Test
    void constructor_emptyLexemeNonEof_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Token(TokenType.IDENT, "", 1, 1));
    }

    @Test
    void constructor_eofWithNonEmptyLexeme_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Token(TokenType.EOF, "abc", 1, 1));
    }
}