package be.jorisg.ultrastarorganizer.commands.diff;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import be.jorisg.ultrastarorganizer.domain.Library;
import be.jorisg.ultrastarorganizer.domain.TrackInfo;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@CommandLine.Command(name = "diff",
        description = "Compare your library against another library or tracklist csv.")
public class DiffCommand implements Runnable {

    @CommandLine.Parameters(index = "0", description = "The library/tracklist to compare with.")
    private File target;

    @Override
    public void run() {
        if ( !target.exists() ) {
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|red ERROR: The given path '" + target.toPath() + "' does not exist. |@"));
            return;
        }
        UltrastarOrganizer.refresh();

        List<String> other;
        if (target.isDirectory()) {
            other = collectFromDirectory();
        } else {
            other = collectFromTracklist();
        }

        compare(other);
    }

    // diff "C:\songlist.csv"

    private List<String> collectFromTracklist() {
        List<String> other = new ArrayList<>();
        try (
                CSVParser parser = CSVParser.parse(target, StandardCharsets.UTF_8, CSVFormat.DEFAULT.withHeader());
        ) {
            Map<String, Integer> headers = parser.getHeaderMap();
            int artistIndex = headers.get("Artist");
            int titleIndex = headers.get("Title");

            for (CSVRecord rec : parser.getRecords() ) {
                String name = rec.get(artistIndex) + " - " + rec.get(titleIndex);
                other.add(name);
            }
        } catch (IOException e) {
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|red ERROR: " + e.getMessage() + "|@"));
            e.printStackTrace(UltrastarOrganizer.out);
        }

        return other;
    }

    private List<String> collectFromDirectory() {
        Library lib = new Library(target);
        return lib.tracks().stream().map(TrackInfo::name).toList();
    }

    private void compare(Collection<String> other) {
        List<String> thiz = UltrastarOrganizer.library().tracks()
                .stream()
                .map(TrackInfo::name)
                .toList();

        List<String> unique = new ArrayList<>(thiz); // only in current library
        unique.removeAll(other);

        List<String> missing = new ArrayList<>(other); // not in current library
        missing.removeAll(thiz);

        List<String> duplicate = new ArrayList<>(thiz); // in both
        duplicate.removeIf(s -> !other.contains(s));

        // unique
        if ( unique.size() > 0 ) {
            UltrastarOrganizer.out.println();
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                    "@|cyan Found " + unique.size() + " unique tracks: |@"));

            unique.forEach(u -> UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                    "@|green + " + u + "|@")));
        }

        // missing
        if ( missing.size() > 0 ) {
            UltrastarOrganizer.out.println();
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                    "@|cyan Found " + missing.size() + " missing tracks: |@"));
            missing.forEach(m -> UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                    "@|red - " + m + "|@")));
        }

        // duplicate
        if ( duplicate.size() > 0 ) {
            UltrastarOrganizer.out.println();
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                    "@|cyan Found " + duplicate.size() + " duplicate tracks: |@"));
            duplicate.forEach(m -> UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                    "@|yellow - " + m + "|@")));
        }

        if ( unique.isEmpty() && missing.isEmpty() && duplicate.isEmpty() ) {
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                    "@|cyan No differences found.|@"));
        }
    }
}
