package fr.younesb.minijaja;

import fr.younesb.minijaja.error.MjjError;
import fr.younesb.minijaja.lexer.LexResult;
import fr.younesb.minijaja.lexer.Lexer;
import fr.younesb.minijaja.lexer.Token;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class Main {
    public static void main(String[] args) {
        boolean tokens = false;
        String fileArg = null;

        for (String arg : args) {
            if (arg.equals("--tokens")) {
                if (tokens) usage("--tokens given more than once");
                tokens = true;
            } else if (arg.startsWith("--")) {
                usage("unknown option: " + arg);
            } else {
                if (fileArg != null) usage("only one file can be given");
                fileArg = arg;
            }
        }
        if (fileArg == null) usage("missing file");

        Path path = Path.of(fileArg);

        String source;
        try {
            source = Files.readString(path, StandardCharsets.UTF_8);
        } catch (CharacterCodingException e) {
            System.err.println("Error: " + path + " is not valid UTF-8 text");
            System.exit(65);
            return;
        } catch (NoSuchFileException e) {
            System.err.println("Error: file not found: " + path);
            System.exit(66);
            return;
        } catch (AccessDeniedException e) {
            System.err.println("Error: permission denied: " + path);
            System.exit(66);
            return;
        } catch (IOException e) {
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            System.err.println("Error: cannot read " + path + ": " + reason);
            System.exit(66);
            return;
        }

        LexResult lexResult = run(source);
        if (lexResult.hasErrors()) {
            for (MjjError e : lexResult.errors()) {
                System.err.println(fileArg + ":" + e.line() + ":" + e.column() + ":" +
                        " error: " + e.code().phase() + " " + e.code() + ": " + e.message());
            }
            System.exit(65);
        }
        if (tokens) {
            for (Token t : lexResult.tokens()) {
                System.out.println(Integer.toString(t.line()) + ':' + t.column() + ' ' + t.type().toString() + ' ' +
                        t.lexeme().replace("\\", "\\\\")
                                .replace("\n", "\\n")
                                .replace("\t", "\\t")
                                .replace("\r", "\\r"));
            }
            System.exit(0);
        }
    }

    public static LexResult run(String source) {
        return Lexer.scan(source);
    }

    private static void usage(String error) {
        System.err.println("Error: " + error);
        System.err.println("usage: minijaja [--tokens] <file>");
        System.exit(64);
    }
}
