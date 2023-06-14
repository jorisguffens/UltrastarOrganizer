package be.jorisg.ultrastarorganizer.commands.automatch;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import be.jorisg.ultrastarorganizer.commands.reformat.ReformatCommand;
import be.jorisg.ultrastarorganizer.domain.Library;
import be.jorisg.ultrastarorganizer.domain.TrackDirectory;
import be.jorisg.ultrastarorganizer.domain.TrackInfo;
import be.jorisg.ultrastarorganizer.search.SearchEngine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@CommandLine.Command(name = "automatch",
        description = "Match the correct mp3 files with the correct info files.")
public class AutomatchCommand implements Runnable {

    @CommandLine.Option(names = {"--dry-run"}, description = "Match audio files with song files but do not update them.")
    private boolean dryRun = false;

    @CommandLine.Option(names = {"--min-match"}, description = "Min percentage for a file to be considerd enough of a match.")
    private double minPercentMatch = 0.5;

    // process after matching
    private final ReformatCommand reformat = new ReformatCommand();

    @Override
    public void run() {
        Library library = UltrastarOrganizer.refresh();

        File[] files = UltrastarOrganizer.workDir.listFiles();
        if (files == null) {
            return;
        }

        // index mp3 files
        SearchEngine<File> engine = new SearchEngine<>();
        Arrays.stream(files).filter(f -> f.getName().endsWith(".mp3"))
                .forEach(f -> {
                    String key = StringEscapeUtils.unescapeHtml4(f.getName());
                    key = key.substring(0, key.length() - 4);
                    engine.index(f, key);
                });

        // match mp3 files with ultrastar tracks
        Map<TrackDirectory, SearchEngine.SearchResult<File>> results = new HashMap<>();
        List<TrackDirectory> failures = new ArrayList<>();

        for (TrackDirectory td : library.trackDirectories()) {
            try {
                TrackInfo main = td.originalTrack();
                if (main.audioFile() != null) {
                    continue;
                }

                File target = new File(td.directory(), main.safeName() + ".mp3");
                if (target.exists()) {
                    reformat.process(main);
                    continue;
                }

                SearchEngine.SearchResult<File> result = process(main, engine).orElse(null);
                if (result != null) {
                    results.put(td, result);
                } else {
                    failures.add(td);
                }
            } catch (Exception e) {
                e.printStackTrace();
                UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|red ERROR: " + e.getMessage() + "|@"));
            }
        }

        // print all matches if dry-run
        if (dryRun) {
            for (TrackDirectory td : results.keySet()) {
                TrackInfo main = td.originalTrack();
                SearchEngine.SearchResult<File> result = results.get(td);
                if (result.score() < minPercentMatch) {
                    UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(String.format(
                            "@|yellow Best audio file match for|@ @|magenta \"%s\"|@ @|yellow has a score of %.2f which is below the treshold of %.2f:|@", main.name(), result.score(), minPercentMatch)));
                    UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(String.format(
                            " ".repeat(27) + "@|magenta %s|@", result.option().getName())));
                } else {
                    UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(String.format(
                            "@|green Found audio file for track|@ @|yellow \"%s\"|@ @|green with score %.2f:|@", main.name(), result.score())));
                    UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(String.format(
                            " ".repeat(28) + "@|magenta %s|@", result.option().getName())));
                }
                UltrastarOrganizer.out.println();
            }
            for (TrackDirectory td : failures) {
                TrackInfo main = td.originalTrack();
                UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(String.format(
                        "@|red Failed to find audio file found for|@ @|magenta \"%s\"|@ @|red .|@", main.name())));
            }
            return;
        }

        // above threshold
        Set<TrackDirectory> match = results.entrySet().stream()
                .filter(e -> e.getValue().score() >= minPercentMatch)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(String.format(
                "@|green Found matching audio files for %d tracks. |@", match.size())));

        Set<TrackDirectory> accepted = new HashSet<>(match);

        // below threshold, ask for confirmation
        Set<TrackDirectory> nomatch = results.entrySet().stream()
                .filter(e -> e.getValue().score() < minPercentMatch)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(String.format(
                "@|yellow There are %d tracks that are below the threshold and need manual confirmation. |@", nomatch.size())));

        for ( TrackDirectory td : nomatch ) {
            if (accepted.contains(td)) {
                continue;
            }

            TrackInfo main = td.originalTrack();
            SearchEngine.SearchResult<File> result = results.get(td);
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(String.format(
                    "@|yellow Best audio file match for|@ @|magenta \"%s\"|@ @|yellow is below the treshold of %.2f:|@", main.name(), result.score())));
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(String.format(
                    " ".repeat(27) + "@|magenta %s|@", result.option().getName())));
            UltrastarOrganizer.out.print("Accept this file? [y/N] ");
            if (!UltrastarOrganizer.in.readLine().equalsIgnoreCase("y")) {
                failures.add(td);
                continue;
            }

            accepted.add(td);
        }

        // copy files
        List<File> copied = new ArrayList<>();
        for (TrackDirectory td : accepted) {
            try {
                TrackInfo main = td.originalTrack();
                SearchEngine.SearchResult<File> result = results.get(td);
                File target = new File(td.directory(), main.safeName() + ".mp3");
                FileUtils.copyFile(result.option(), target);
                reformat.process(main);

                copied.add(result.option());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // delete files
        for ( File file : copied ) {
            file.delete();
        }

        if (!failures.isEmpty()) {
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(String.format(
                    "@|red Failed to find matching audio files for %d tracks: |@", failures.size())));
            for (TrackDirectory td : failures) {
                TrackInfo main = td.originalTrack();
                UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(String.format(
                        "@|red Failed to find audio file found for|@ @|magenta \"%s\"|@ @|red .|@", main.name())));
            }
        }

        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                "@|cyan Finished automatching audio files with sub-directories. |@"));
    }

    private Optional<SearchEngine.SearchResult<File>> process(TrackInfo main, SearchEngine<File> engine) throws IOException {
        SearchEngine.SearchResult<File> result = engine.searchOne(main.safeName());
        if (result == null) {
            return Optional.empty();
        }
        return Optional.of(result);
    }

}
