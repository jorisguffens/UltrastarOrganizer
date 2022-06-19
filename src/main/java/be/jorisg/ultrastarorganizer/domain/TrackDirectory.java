package be.jorisg.ultrastarorganizer.domain;

import java.io.File;
import java.util.Collection;
import java.util.List;

public class TrackDirectory {

    private final File directory;
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
}
