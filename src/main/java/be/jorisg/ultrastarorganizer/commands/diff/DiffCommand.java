package be.jorisg.ultrastarorganizer.commands.diff;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import be.jorisg.ultrastarorganizer.domain.Library;
import be.jorisg.ultrastarorganizer.domain.TrackInfo;
import be.jorisg.ultrastarorganizer.utils.Utils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.*;

@CommandLine.Command(name = "diff",
        description = "Compare current library against another librarv.")
public class DiffCommand implements Runnable {

    @CommandLine.Parameters(index = "0", description = "The library directory to compare with.")
    private File target;

    @CommandLine.Option(names = {"-c", "--copy-to"}, description = "Copy the shown files to the given directory.")
    private File copyTo;

    @CommandLine.Option(names = {"-d", "--show-duplicates"}, description = "Show all tracks that are both in the current library and the given library.")
    private boolean showDuplicates = false;

    @CommandLine.Option(names = {"-m", "--show-missing"}, description = "Show all tracks that are in the given library but not in the current library.")
    private boolean showMissing = false;

    @CommandLine.Option(names = {"-u", "--show-unique"}, description = "Show all tracks that are in the current library but not in the given library.")
    private boolean showUnique = false;

    @Override
    public void run() {
        if (!target.exists()) {
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|red ERROR: The given path '" + target.toPath() + "' does not exist. |@"));
            return;
        }
        UltrastarOrganizer.refresh();

        Library lib = new Library(target);
        isSimilar(lib.tracks());
    }

    private boolean isSimilar(String s, String t) {
        return LevenshteinDistance.getDefaultInstance().apply(s.toLowerCase(), t.toLowerCase()) < 2;
    }

    private void isSimilar(Collection<TrackInfo> other) {
        List<TrackInfo> thiz = UltrastarOrganizer.library().tracks();

        List<TrackInfo> unique = new ArrayList<>(thiz); // only in current library
        unique.removeIf(ti -> other.stream().anyMatch(t -> isSimilar(t.name(), ti.name())));

        List<TrackInfo> missing = new ArrayList<>(other); // not in current library
        missing.removeIf(ti -> thiz.stream().anyMatch(t -> isSimilar(t.name(), ti.name())));

        List<TrackInfo> duplicate = new ArrayList<>(thiz); // in both
        duplicate.removeIf(ti -> other.stream().noneMatch(t -> isSimilar(t.name(), ti.name())));
        Set<File> copied = new HashSet<>();

        // unique
        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                "@|cyan Found " + unique.size() + " unique tracks. |@"));
        if (showUnique) {
            unique.forEach(ti -> UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                    "@|green + " + ti.name() + "|@")));
            UltrastarOrganizer.out.println();

            if (copyTo != null) {
                copyTo(unique, copied);
            }
        }

        // missing
        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                "@|cyan Found " + missing.size() + " missing tracks. |@"));
        if (showMissing) {
            missing.forEach(ti -> UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                    "@|red - " + ti.name() + "|@")));
            UltrastarOrganizer.out.println();

            if (copyTo != null) {
                copyTo(missing, copied);
            }
        }

        // duplicate
        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                "@|cyan Found " + duplicate.size() + " duplicate tracks. |@"));
        if (showDuplicates) {
            duplicate.forEach(ti -> UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                    "@|yellow * " + ti.name() + "|@")));
            UltrastarOrganizer.out.println();

            if (copyTo != null) {
                copyTo(duplicate, copied);
            }
        }
    }

    private void copyTo(List<TrackInfo> tracks, Set<File> copied) {
        tracks.stream()
                .map(TrackInfo::parentDirectory)
                .distinct()
                .filter(td -> !copied.contains(td))
                .forEach(td -> {
                    copied.add(td);
                    try {
                        Utils.copyFolder(td.toPath(), copyTo.toPath().resolve(td.getName()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
