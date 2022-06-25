package be.jorisg.ultrastarorganizer.domain;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TrackDirectory {

    private File directory;
    private final List<TrackInfo> tracks;

    public TrackDirectory(File directory, Collection<TrackInfo> tracks) {
        this.directory = directory;
        this.tracks = List.copyOf(tracks);
    }

    public File directory() {
        return directory;
    }

    public List<TrackInfo> tracks() {
        return tracks;
    }

    public TrackInfo originalTrack() {
        return tracks.stream().min(Comparator.comparing(ti -> ti.isDuet() ? 1 : 0)).orElse(null);
    }

    public void moveTo(File dest) throws IOException {
        if ( dest.exists() ) {
            FileUtils.deleteDirectory(dest);
        }
        FileUtils.moveDirectory(directory, dest);
        this.directory = dest;
        UltrastarOrganizer.out.println("Moving " + directory.getName() + " to " + dest.getName() + ".");
//        File tmp = new File(dest.getParent(), "[TMP] " + dest.getName());
//        FileUtils.copyDirectory(directory, tmp);
//        FileUtils.deleteDirectory(directory);
//        FileUtils.copyDirectory(tmp, dest);
//        FileUtils.deleteDirectory(tmp);
    }
}
