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
import java.util.Arrays;

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

        SearchEngine<File> engine = new SearchEngine<>();
        Arrays.stream(files).filter(f -> f.getName().endsWith(".mp3"))
                .forEach(f -> {
                    String key = StringEscapeUtils.unescapeHtml4(f.getName());
                    key = key.substring(0, key.length() - 4);
                    engine.indexer.accept(f, key);
                });

        for (TrackDirectory td : library.trackDirectories()) {
            try {
                process(td, engine);
            } catch (Exception e) {
                UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|red ERROR: " + e.getMessage() + "|@"));
            }
        }
    }

    private void process(TrackDirectory td, SearchEngine<File> engine) throws IOException {
        TrackInfo main = td.originalTrack();
        if ( main.audioFile() != null ) {
            return;
        }

        SearchEngine.SearchResult<File> result = engine.searchOne(main.safeName());
        if (result == null ) {
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|yellow WARNING: No match found for " + main.safeName() + "|@"));
            return;
        }

        if ( result.pctMatch() < minPercentMatch || result.match() < 1 ) {
            String s = String.format("@|yellow WARNING: Best match for %s has a score of (%.2f) which is below the treshold of %.2f:\n\t%s|@",
                    main.safeName(), result.pctMatch(), minPercentMatch, result.option().getName());
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(s));
            return;
        }

        engine.removeIndex(result.option());

        if ( dryRun ) {
            UltrastarOrganizer.out.printf("Found file for track %s with score %.2f:\n\t%s\n", main.name(), result.match(), result.option().getName());
            return;
        }

        FileUtils.moveFile(result.option(), new File(td.directory(), main.safeName() + ".mp3"));
        reformat.process(main);
    }

}
