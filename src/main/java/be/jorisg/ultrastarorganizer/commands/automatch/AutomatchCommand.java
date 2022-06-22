/*
 * This file is part of Ultrastar Organizer, licensed under the MIT License.
 *
 * Copyright (c) Joris Guffens
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package be.jorisg.ultrastarorganizer.commands.automatch;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import be.jorisg.ultrastarorganizer.commands.reformat.ReformatCommand;
import be.jorisg.ultrastarorganizer.commands.tracklist.TracklistType;
import be.jorisg.ultrastarorganizer.domain.Library;
import be.jorisg.ultrastarorganizer.domain.TrackDirectory;
import be.jorisg.ultrastarorganizer.domain.TrackInfo;
import be.jorisg.ultrastarorganizer.search.SearchEngine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;
import picocli.CommandLine;

import javax.naming.directory.SearchResult;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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

        SearchEngine.SearchResult<File> result = engine.search(main.safeName());
        if (result == null ) {
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|red No match found for " + main.safeName() + "|@"));
            return;
        }

        if ( result.pctMatch() < minPercentMatch || result.match() < 1 ) {
            String s = String.format("@|red Best match for %s has a score of (%.2f) which is below the treshold of %.2f:\n\t%s|@",
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
