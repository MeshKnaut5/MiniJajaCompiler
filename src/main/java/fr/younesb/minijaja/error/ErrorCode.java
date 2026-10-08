package fr.younesb.minijaja.error;

public enum ErrorCode {
    LONE_AMPER(Phase.LEXICAL), LONE_PIPE(Phase.LEXICAL), UNEXPECTED_CHARACTER(Phase.LEXICAL),
    UNTERMINATED_STRING(Phase.LEXICAL), UNTERMINATED_BLOCK_COMMENT(Phase.LEXICAL);

    private final Phase phase;

    ErrorCode(Phase phase) {
        this.phase = phase;
    }

    public Phase phase() {
        return phase;
    }
}
