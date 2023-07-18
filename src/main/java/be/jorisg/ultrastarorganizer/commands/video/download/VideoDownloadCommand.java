package be.jorisg.ultrastarorganizer.commands.video.download;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import be.jorisg.ultrastarorganizer.commands.minimize.MinimizeCommand;
import be.jorisg.ultrastarorganizer.domain.TrackInfo;
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
import java.util.Comparator;
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
        YoutubeDownloader downloader = init();

        UltrastarOrganizer.refresh();
        UltrastarOrganizer.library().tracks().forEach(ti -> process(downloader, ti));
    }

    private void process(YoutubeDownloader dl, TrackInfo ti) {
        if ( ti.videoFile() != null && ti.videoFile().exists() ) {
            return;
        }

        RequestSearchResult rsr = new RequestSearchResult(ti.name() + " VIDEO")
                .type(TypeField.VIDEO)
                .forceExactQuery(true)
                .sortBy(SortField.RELEVANCE);

        SearchResult result = dl.search(rsr).data();
        List<SearchResultVideoDetails> videos = result.videos();
        if (videos.isEmpty()) {
            UltrastarOrganizer.out.printf(CommandLine.Help.Ansi.AUTO
                    .string("@|red ERROR: No valid video found for %s.|@\n"), ti.safeName());
            return;
        }

        RequestVideoInfo request = new RequestVideoInfo(videos.get(0).videoId());
        VideoInfo video = dl.getVideoInfo(request).data();
        VideoFormat videoFormat = video.videoFormats().stream()
                .filter(vf -> vf.videoQuality().ordinal() <= VideoQuality.hd1080.ordinal())
                .max(Comparator.comparingInt(vf -> vf.videoQuality().ordinal()))
                .orElseThrow();

        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(String.format(
                "@|yellow Found video for|@ @|magenta %s|@@|yellow :|@", ti.name())));
        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(String.format(
                " ".repeat(16) + "@|magenta %s: %s|@ @|red (%s) |@",
                video.details().author(), video.details().title(), videoFormat.qualityLabel())));

        if ( dryRun ) {
            return;
        }

        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(String.format(
                "@|yellow Downloading video for |@ @|magenta \"%s\"|@ @|yellow .|@", ti.name())));
        RequestVideoFileDownload rvfd = new RequestVideoFileDownload(videoFormat);
        File src = dl.downloadVideoFile(rvfd).data();

        try {
            File dest = new File(ti.parentDirectory(), ti.safeName() + ".mp4");
            FileUtils.moveFile(src, dest);

            ti.setVideoFileName(dest.getName());

            MinimizeCommand.video(ti);

            ti.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
