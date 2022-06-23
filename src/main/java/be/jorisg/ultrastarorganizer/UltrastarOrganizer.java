package be.jorisg.ultrastarorganizer;

import be.jorisg.ultrastarorganizer.command.CliCommands;
import be.jorisg.ultrastarorganizer.domain.Library;
import org.fusesource.jansi.AnsiConsole;
import org.jline.reader.LineReader;
import picocli.CommandLine;

import java.io.File;
import java.io.PrintWriter;

public class UltrastarOrganizer {

    public static File workDir;
    public static PrintWriter out;
    public static LineReader in;

    private static Library library;

    public static Library library() {
        if (library == null) {
            return refresh();
        }
        return library;
    }

    public static Library refresh() {
        library = new Library(workDir);
        return library;
    }

    //

    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        new CommandLine(new CliCommands()).execute(args);
        AnsiConsole.systemUninstall();
    }

}
