package be.jorisg.ultrastarorganizer;

import be.jorisg.ultrastarorganizer.commands.Tracklist;
import be.jorisg.ultrastarorganizer.domain.Library;
import be.jorisg.ultrastarorganizer.domain.TrackInfo;
import be.jorisg.ultrastarorganizer.terminal.Console;
import org.apache.commons.io.IOUtils;
import org.jline.reader.UserInterruptException;
import org.jline.utils.AttributedStyle;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public class Main2 {

    public static void main(String[] args) {
        try {
            run();
        } catch(UserInterruptException ignored) {}
    }

    private static void run() {
        try (
                InputStream is = Main2.class.getClassLoader().getResourceAsStream("greeting.txt");
        ) {
            String greeting = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
            Console.get().write(greeting);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Console.println("Let's start by getting you setup!\n");

        // setup: get directory
        File directory;
        while (true) {
            directory = Console.ask(Console.style("Library directory: ", AttributedStyle.WHITE), File.class);

            if (directory == null || !directory.isDirectory()) {
                Console.printError("Invalid library directory.");
                continue;
            }

            break;
        }

        Library library = new Library(directory);
        List<TrackInfo> tracks = library.tracks();
        Console.println(Console.style(String.format("Loaded %d tracks in %s directories of which %d are duets.\n",
                tracks.size(),
                library.trackDirectories().size(),
                tracks.stream().filter(TrackInfo::isDuet).count()), AttributedStyle.CYAN));

        Console.println("Time to unleash the power!");

        CommandLine cli = new CommandLine(new ParentCommand());
        cli.addSubcommand(new Tracklist(library));

        Console.get().start(cli);
    }

    @CommandLine.Command
    private static class ParentCommand {}

}
