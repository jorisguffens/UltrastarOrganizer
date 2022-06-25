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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
        List<String> unique = new ArrayList<>(); // only in current library
        List<String> missing = new ArrayList<>(); // not in current library

        for (TrackInfo ti : UltrastarOrganizer.library().tracks() ) {
            if ( !other.contains(ti.name()) ) {
                unique.add(ti.name());
            }
        }

        for ( String o : other ) {
            if ( UltrastarOrganizer.library().tracks().stream().noneMatch(ti -> ti.name().equals(o)) ) {
                missing.add(o);
            }
        }

        // unique
        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                "@|cyan Found " + unique.size() + " unique tracks: |@"));

        unique.forEach(u -> UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                "@|green + " + u + "|@")));

        // missing
        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                "@|cyan Found " + missing.size() + " missing tracks: |@"));
        missing.forEach(m -> UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                "@|red - " + m + "|@")));

    }
}
