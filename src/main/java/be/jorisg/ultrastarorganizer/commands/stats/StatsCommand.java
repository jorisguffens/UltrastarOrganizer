package be.jorisg.ultrastarorganizer.commands.stats;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import be.jorisg.ultrastarorganizer.domain.Library;
import be.jorisg.ultrastarorganizer.domain.TrackDirectory;
import be.jorisg.ultrastarorganizer.domain.TrackInfo;
import info.aduna.io.FileUtil;
import org.apache.commons.io.FileUtils;
import org.fusesource.jansi.Ansi;
import picocli.CommandLine;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@CommandLine.Command(name = "stats",
        description = "Show stats of your library")
public class StatsCommand implements Runnable {

    @Override
    public void run() {
        Library library = UltrastarOrganizer.refresh();

        Map<String, String> stats = new LinkedHashMap<>();
        stats.put("Directories", library.trackDirectories().size() + "");
        stats.put("Total Tracks", library.tracks().size() + "");
        stats.put("Duets", library.tracks().stream().filter(TrackInfo::isDuet).count() + "");
        stats.put("Disney songs", library.tracks().stream().filter(ti -> ti.artist().contains("Disney")).count() + "");

        long size = FileUtils.sizeOfDirectory(library.directory());
        stats.put("Total size", String.format("%.2f GB", size / 1024.d / 1024.d / 1024.d));
        stats.put("Average size", String.format("%.2f MB", size / library.trackDirectories().size() / 1024.d / 1024.d));

        for ( String key : stats.keySet() ) {
            UltrastarOrganizer.out.print(CommandLine.Help.Ansi.AUTO.string("@|cyan " + key + ": |@"));
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|white " + stats.get(key) + "|@"));
        }
    }

}
