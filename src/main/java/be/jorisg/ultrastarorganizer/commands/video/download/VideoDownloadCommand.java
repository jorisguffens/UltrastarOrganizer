package be.jorisg.ultrastarorganizer.commands.video.download;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import be.jorisg.ultrastarorganizer.domain.TrackDirectory;
import be.jorisg.ultrastarorganizer.domain.TrackInfo;
import be.jorisg.ultrastarorganizer.utils.Tasker;
import be.jorisg.ultrastarorganizer.utils.Utils;
import com.github.kiulian.downloader.Config;
import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.request.RequestSearchResult;
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.model.search.SearchResult;
import com.github.kiulian.downloader.model.search.SearchResultVideoDetails;
import com.github.kiulian.downloader.model.search.field.SortField;
import com.github.kiulian.downloader.model.search.field.TypeField;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.VideoFormat;
import com.github.kiulian.downloader.model.videos.quality.VideoQuality;
import org.apache.commons.io.FileUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

@CommandLine.Command(name = "download",
        description = "Download videos from youtube.")
public class VideoDownloadCommand implements Runnable {

    @CommandLine.Option(names = {"--proxy", "-p"}, description = "Download through proxy.")
    private String proxy;

    @CommandLine.Option(names = {"--dry-run"}, description = "Search for videos but don't download them.")
    private boolean dryRun = false;

    private YoutubeDownloader init() {
        Config.Builder cb = new Config.Builder();
        cb.maxRetries(0);
//        cb.header("Accept-language", "en-US,en;");

        // proxy
        if (proxy != null) {
            String[] parts = proxy.split("[:@]");
            if (parts.length != 4) {
                throw new IllegalArgumentException("Proxy should be in format: username:password@host:port");
            }

            cb.proxy(parts[2], Integer.parseInt(parts[3]), parts[0], parts[1]);
        }

        Config config = cb.build();
        return new YoutubeDownloader(config);
    }

    @Override
    public void run() {
        YoutubeDownloader dl = init();

        UltrastarOrganizer.refresh();

        PrintStream original = System.out;
        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                // do nothing
            }
        }));

        Tasker tasker = new Tasker(tasks(dl), 5);
        tasker.start();
//        tasker.join();
    }

    private Iterator<Tasker.Task> tasks(YoutubeDownloader dl) {
        return new Iterator<>() {
            private int index = 0;
            private final List<TrackDirectory> dirs = UltrastarOrganizer.library().trackDirectories();

            @Override
            public boolean hasNext() {
                return index < dirs.size() - 1;
            }

            @Override
            public Tasker.Task next() {
                if (!hasNext()) {
                    throw new IndexOutOfBoundsException();
                }
                final TrackDirectory td = dirs.get(index++);
                return new Tasker.Task(td.originalTrack().name(), () -> {
                    List<String> msg = new ArrayList<>();
                    process(dl, td, msg);

                    if (!msg.isEmpty()) {
                        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(String.format(
                                "@|cyan Information about|@ @|magenta %s|@@|cyan :|@", td.directory().getName())));
                        msg.forEach(s -> UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO
                                .string(String.format("@|yellow  - %s|@", s))));
                    }
                });
            }
        };
    }

    private void process(YoutubeDownloader dl, TrackDirectory td, List<String> msg) {
        TrackInfo ti = td.originalTrack();
        if (ti.videoFile() != null && ti.videoFile().exists()) {
            return;
        }

        File dest = new File(ti.parentDirectory(), ti.safeName() + ".mp4");
        if (dest.exists()) {
            msg.add("Destination video file exists but was not listed in track info.");
            return;
        }

        RequestSearchResult rsr = new RequestSearchResult(ti.name() + " VIDEO")
                .type(TypeField.VIDEO)
                .forceExactQuery(true)
                .sortBy(SortField.RELEVANCE);
        SearchResult result = dl.search(rsr).data();

        List<SearchResultVideoDetails> videos = result.videos();
        if (videos.isEmpty()) {
            msg.add("No valid video found.");
            return;
        }

        RequestVideoInfo request = new RequestVideoInfo(videos.get(0).videoId());
        VideoInfo video = dl.getVideoInfo(request).data();

        if (video == null) {
            msg.add("Failed to retrieve video information.");
            return;
        }
        VideoFormat videoFormat = video.videoFormats().stream()
                .filter(vf -> vf.videoQuality().ordinal() <= VideoQuality.hd1080.ordinal())
                .max(Comparator.comparingInt(vf -> vf.videoQuality().ordinal()))
                .orElseThrow();

        msg.add(String.format("Found video: %s: %s|@ @|red (%s) |@@|green ",
                video.details().author(), video.details().title(), videoFormat.qualityLabel()));

        if (dryRun) {
            return;
        }

        RequestVideoFileDownload rvfd = new RequestVideoFileDownload(videoFormat);
        File src = dl.downloadVideoFile(rvfd).data();

        msg.add("Downloaded video.");

        try {
            FileUtils.moveFile(src, dest);

            ti.setVideoFileName(dest.getName());
            ti.save();

            Utils.shrinkVideo(ti);

            msg.add("Compressed video.");

            for (TrackInfo t : td.tracks()) {
                if (t.equals(ti)) continue;
                t.setVideoFileName(dest.getName());
                t.save();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
