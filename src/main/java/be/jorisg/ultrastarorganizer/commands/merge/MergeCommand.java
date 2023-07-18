package be.jorisg.ultrastarorganizer.commands.merge;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import be.jorisg.ultrastarorganizer.domain.Library;
import be.jorisg.ultrastarorganizer.domain.TrackInfo;
import org.apache.commons.io.FileUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@CommandLine.Command(name = "merge",
        description = "Add unique songs from another library to the current library.")
public class MergeCommand implements Runnable {

    @CommandLine.Parameters(index = "0", description = "The library to compare with.")
    private File target;

    @Override
    public void run() {
        if (!target.exists()) {
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|red ERROR: The given path '" + target.toPath() + "' does not exist. |@"));
            return;
        }
        if (!target.isDirectory()) {
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|red ERROR: The given path '" + target.toPath() + "' is not a directory. |@"));
            return;
        }
        UltrastarOrganizer.refresh();

        List<TrackInfo> thiz = UltrastarOrganizer.library().tracks()
                .stream()
                .toList();

        Library lib = new Library(target);
        List<TrackInfo> missing = new ArrayList<>(lib.tracks());
        missing.removeIf(ti -> thiz.stream().anyMatch(t -> t.safeName().equals(ti.safeName()))); // not in current library

        Set<File> copied = new HashSet<>();
        for ( TrackInfo ti : missing ) {
            File dir = ti.parentDirectory();
            if ( copied.contains(dir) ) {
                continue;
            }
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|yellow Copying " + dir.getName() + " |@"));
            try {
                FileUtils.copyDirectory(dir, new File(UltrastarOrganizer.library().directory(), dir.getName()), true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            copied.add(dir);
        }

        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|yellow Copied a total of " + missing.size() + " songs. |@"));
    }
}
