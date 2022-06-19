package be.jorisg.ultrastarorganizer.terminal;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.jline.console.impl.SystemHighlighter;
import org.jline.reader.EndOfFileException;
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultHighlighter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

public class Console {

    private static Console console;

    public static Console get() {
        if (console == null) {
            console = new Console();
        }
        return console;
    }

    public static void print(String msg) {
        get().write(msg);
    }

    public static void println(String msg) {
        print(msg + "\n");
    }

    public static void printError(String msg) {
        println(style("! " + msg, AttributedStyle.RED));
    }

    public static void printInfo(String msg) {
        println(style(msg, AttributedStyle.CYAN));
    }

    public static <T> T ask(String prompt, Class<T> type) {
        String color = Ansi.ansi().fgRgb(Integer.parseInt("258f24", 16)).toString();
        return get().readLine(style("? ", "#258f24") + style(prompt, AttributedStyle.WHITE) + color, type);
    }

    //

    private static final PrintStream stdout = System.out;

    private final Terminal terminal;
    private final LineReader reader;

    private Console() {
        boolean dumb = System.getProperty("terminal.dumb", "false").equals("true");

        // IDE's use a dumb terminal
        String IDEpath = Path.of("build/classes/java/main").toString();
        if ( System.getProperty("java.class.path").contains(IDEpath) ) {
            dumb = true;
        }

        try {
            AnsiConsole.systemInstall();
            terminal = TerminalBuilder.builder().dumb(dumb).color(true).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        reader = LineReaderBuilder.builder().terminal(terminal).highlighter(new Highlighter() {
            public AttributedString highlight(LineReader reader, String buffer) {
                return new AttributedString(buffer, AttributedStyle.DEFAULT.foregroundRgb(Integer.parseInt("d8d8d8", 16)));
            }

            @Override
            public void setErrorPattern(Pattern errorPattern) {}

            @Override
            public void setErrorIndex(int errorIndex) {}
        }).build();
        reader.setOpt(LineReader.Option.DISABLE_EVENT_EXPANSION);
        reader.unsetOpt(LineReader.Option.INSERT_TAB);
    }

    public void write(String msg) {
        if (terminal == null) {
            stdout.print(msg);
            return;
        }

        if (reader == null) {
            terminal.writer().print(msg);
            terminal.writer().flush();
            return;
        }

        reader.printAbove(msg);
    }

    public String readLine(String prompt) {
        String input = reader.readLine(prompt);
        if ( input != null ) {
            input = input.trim();
        }
        return input;
    }

    private final static Map<Class<?>, Function<String, ?>> transformers = new HashMap<>();
    static {
        transformers.put(File.class, File::new);
        transformers.put(Integer.class, Integer::parseInt);
        transformers.put(Double.class, Double::parseDouble);
        transformers.put(Float.class, Float::parseFloat);
        transformers.put(Boolean.class, in -> Boolean.parseBoolean(in.toLowerCase()));
    }

    public <T> T readLine(String prompt, Class<T> type) {
        if ( transformers.containsKey(type) ) {
            return type.cast(readLine(prompt, transformers.get(type)));
        }
        throw new IllegalArgumentException("No default transformer exists for the given class.");
    }

    public <T> T readLine(String prompt, Function<String, T> transformer) {
        String input = readLine(prompt);
        if ( input == null ) {
            return null;
        }
        try {
            return transformer.apply(input);
        } catch (Exception ex) {
            return null;
        }
    }

    public void start(CommandLine cli) {
        cli.setErr(terminal.writer());

        String line;
        while (true) {
            try {
                line = reader.readLine("> ");
            } catch (EndOfFileException ignored) {
                continue;
            }

            if (line == null) {
                break;
            }

            line = line.trim();
            cli.execute(line.split(Pattern.quote(" ")));
        }
    }

    //

    public static String style(String text, String hex) {
        if ( hex.startsWith("#") ) hex = hex.substring(1);
        return style(text, AttributedStyle.DEFAULT.foreground(Integer.parseInt(hex, 16)));
    }

    public static String style(String text, int color) {
        return style(text, AttributedStyle.DEFAULT.foreground(color));
    }

    public static String style(String text, AttributedStyle style) {
        return new AttributedStringBuilder()
                .style(style)
                .append(text)
                .style(AttributedStyle.DEFAULT)
                .toAnsi();
    }

}
