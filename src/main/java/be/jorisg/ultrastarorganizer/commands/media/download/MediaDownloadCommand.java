package be.jorisg.ultrastarorganizer.commands.media.download;

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
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.search.SearchResult;
import com.github.kiulian.downloader.model.search.SearchResultVideoDetails;
import com.github.kiulian.downloader.model.search.field.SortField;
import com.github.kiulian.downloader.model.search.field.TypeField;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.AudioFormat;
import com.github.kiulian.downloader.model.videos.formats.Format;
import com.github.kiulian.downloader.model.videos.formats.VideoFormat;
import com.github.kiulian.downloader.model.videos.quality.VideoQuality;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

@CommandLine.Command(name = "download",
        description = "Download video/audio from youtube.")
public class MediaDownloadCommand implements Runnable {

    @CommandLine.Option(names = {"--proxy", "-p"}, description = "Download through proxy.")
    private String proxy;

    @CommandLine.Option(names = {"--dry-run"}, description = "Search for videos but don't download them.")
    private boolean dryRun = false;

    @CommandLine.Option(required = true, names = {"--type"}, description = "Specify which media type to download. Options are: VIDEO, AUDIO")
    private MediaType type;

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
        tasker.join();

        System.setOut(original);
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

        // pre check
        boolean skip = switch (type) {
            case VIDEO -> ti.videoFile() != null;
            case AUDIO -> ti.audioFile() != null;
        };

        if (skip) {
            return;
        }

        // search
        VideoInfo video;
        try {
            video = search(dl, ti);
        } catch (VideoSearchException e) {
            msg.add(e.getMessage());
            return;
        }

        // get format
        final Format format = switch (type) {
            case VIDEO -> video.videoFormats().stream()
                    .filter(vf -> vf.videoQuality().ordinal() <= VideoQuality.hd1080.ordinal())
                    .max(Comparator.<VideoFormat>comparingInt(vf -> vf.videoQuality().ordinal())
                            .thenComparing(Comparator.comparingInt(Format::bitrate).reversed()))
                    .orElseThrow();
            case AUDIO -> video.audioFormats().stream()
                    .max(Comparator.<AudioFormat>comparingInt(vf -> vf.audioQuality().ordinal())
                            .thenComparing(Comparator.comparingInt(Format::bitrate).reversed()))
                    .orElseThrow();
        };

        String quality = switch (format) {
            case VideoFormat vf -> vf.videoQuality().name();
            case AudioFormat af -> af.audioQuality().name();
            default -> throw new IllegalStateException("Unexpected value: " + format);
        };

        msg.add(String.format("Found " + type.name() + ": %s: |@@|cyan %s|@ @|red (%s) |@@|green ",
                video.details().author(), video.details().title(), quality));

        if (dryRun) {
            return;
        }

        try {
            // download
            File file = download(dl, format, ti, msg);

            if (type == MediaType.AUDIO) {
                File dest = new File(ti.parentDirectory(), ti.safeName() + ".mp3");
                Utils.extractAudioFromVideo(file, dest);
                file = dest;
            } else {
                // move video to track directory
                String ext = FilenameUtils.getExtension(file.getName());
                File dest = new File(ti.parentDirectory(), ti.safeName() + "." + ext);
                FileUtils.moveFile(file, dest);
                file = dest;
            }

            // update track info
            BiConsumer<TrackInfo, String> setter = switch (type) {
                case VIDEO -> TrackInfo::setVideoFileName;
                case AUDIO -> TrackInfo::setAudioFileName;
            };

            for (TrackInfo t : td.tracks()) {
                setter.accept(t, file.getName());
                t.save();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private VideoInfo search(YoutubeDownloader dl, TrackInfo ti) throws VideoSearchException {
        RequestSearchResult rsr = new RequestSearchResult(ti.name() + " VIDEO")
                .type(TypeField.VIDEO)
                .forceExactQuery(true)
                .sortBy(SortField.RELEVANCE);
        Response<SearchResult> response = dl.search(rsr);
        if ( response == null ) {
            throw new VideoSearchException("Failed to search youtube for matching video.");
        }

        SearchResult result;
        try {
            result = response.data(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new VideoSearchException("Failed to search youtube for matching video: " + e.getMessage());
        }

        List<SearchResultVideoDetails> videos = result.videos();
        if (videos.isEmpty()) {
            throw new VideoSearchException("No videos returned in search request.");
        }

        SearchResultVideoDetails vd = videos.stream()
                .filter(v -> !v.title().toLowerCase().contains("lyrics"))
                .findFirst().orElse(null);
        if ( vd == null ) {
            throw new VideoSearchException("Found video results but none are fitting.");
        }

        RequestVideoInfo request = new RequestVideoInfo(vd.videoId());
        VideoInfo video = dl.getVideoInfo(request).data();

        if (video == null) {
            throw new VideoSearchException("Failed to retrieve video information.");
        }

        return video;
    }

    private File download(YoutubeDownloader dl, Format format, TrackInfo ti, List<String> msg) throws VideoDownloadException {
        RequestVideoFileDownload rvfd = new RequestVideoFileDownload(format);
        Response<File> resp = dl.downloadVideoFile(rvfd);
        File src;

        try {
            src = resp.data(2, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            throw new VideoDownloadException("Failed downloading " + type.name() + ": " + e.getMessage());
        }

        if (!resp.ok()) {
            throw new VideoDownloadException("Failed downloading " + type.name() + ": " + resp.error().getMessage());
        }

        msg.add("Downloaded " + type.name() + ".");

        try {
            return src;
        } catch (Exception e) {
            throw new VideoDownloadException("Failed moving " + type.name() + ": " + e.getMessage());
        }
    }

}
