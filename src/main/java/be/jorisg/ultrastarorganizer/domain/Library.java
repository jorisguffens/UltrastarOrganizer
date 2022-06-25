package be.jorisg.ultrastarorganizer.domain;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class Library {

    private final File directory;
    private final List<TrackDirectory> trackDirectories = new ArrayList<>();

    public Library(File directory) {
        this.directory = directory;
        refresh();
    }

    public File directory() {
        return directory;
    }

    public void refresh() {
        File[] files = directory.listFiles();
        if (files == null) {
            System.out.println("Error while loading library files.");
            return;
        }
        ProgressBarBuilder pbb = new ProgressBarBuilder()
                .setTaskName("Loading Library")
                .setInitialMax(files.length)
                .setMaxRenderedLength(100)
                .setStyle(ProgressBarStyle.ASCII);
        try (ProgressBar pb = pbb.build()) {
            for (File file : files) {
                if (!file.isDirectory()) {
                    continue;
                }

                loadTracks(file);
                pb.step();
            }
        }
    }

    private void loadTracks(File directory) {
        List<File> txtFiles = filesByExtensions(directory, "txt");
        List<TrackInfo> tracks = new ArrayList<>();
        for (File file : txtFiles) {
            try {
                tracks.add(TrackInfo.load(file));
            } catch (Exception ignored) {
            }
        }

        if (tracks.isEmpty()) {
            return;
        }

        this.trackDirectories.add(new TrackDirectory(directory, tracks));
    }

    public static List<File> filesByExtensions(File directory, String... extensions) {
        List<String> exts = List.of(extensions);
        return Arrays.stream(Objects.requireNonNull(directory.listFiles())).filter(file -> {
            String ext = FilenameUtils.getExtension(file.getName());
            return exts.contains(ext);
        }).collect(Collectors.toList());
    }

    public List<TrackInfo> tracks() {
        return trackDirectories.stream().flatMap(td -> td.tracks().stream()).collect(Collectors.toList());
    }

    public List<TrackDirectory> trackDirectories() {
        return Collections.unmodifiableList(trackDirectories);
    }

    public TrackDirectory directoryOf(TrackInfo ti) {
        return trackDirectories.stream().filter(td -> td.tracks().contains(ti)).findFirst().orElse(null);
    }
}
