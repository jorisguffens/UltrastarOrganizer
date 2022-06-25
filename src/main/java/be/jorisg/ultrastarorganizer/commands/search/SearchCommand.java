package be.jorisg.ultrastarorganizer.commands.search;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import be.jorisg.ultrastarorganizer.domain.TrackInfo;
import be.jorisg.ultrastarorganizer.search.SearchEngine;
import picocli.CommandLine;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

@CommandLine.Command(name = "search",
        description = "Search tracks by query and/or flags")
public class SearchCommand implements Runnable {

    @CommandLine.Option(names = {"-q", "--query"}, description = "Show tracks that match the given query.")
    private String query;

    @CommandLine.Option(names = {"--has-background"}, description = "Only show tracks with a background image")
    private boolean hasBackground;

    @CommandLine.Option(names = {"--sus-image-ratio"}, description = "Only show tracks with a larger cover file than background.")
    private boolean suspiciousImageRatio;

    @CommandLine.Option(names = {"--multi-versions"}, description = "Only show tracks with multiple versions in the same directory.")
    private boolean multipleVersions;

    @CommandLine.Option(names = {"--duet"}, description = "Only show tracks that are duets.")
    private boolean duet;

    @Override
    public void run() {
        UltrastarOrganizer.refresh();

        List<TrackInfo> result;

        if (query != null) {
            SearchEngine<TrackInfo> engine = new SearchEngine<>();
            UltrastarOrganizer.library().tracks().forEach(ti -> engine.indexer.accept(ti, ti.safeName()));
            result = engine.search(query).stream().map(SearchEngine.SearchResult::option).toList();
        } else {
            result = UltrastarOrganizer.library().tracks();
        }

        if (hasBackground) {
            result = result.stream().filter(trackInfo -> trackInfo.backgroundImageFile() != null).toList();
        }

        if ( suspiciousImageRatio ) {
            result = result.stream().filter(this::suspiciousImageRatio).toList();
        }

        if ( multipleVersions ) {
            result = result.stream().filter(ti -> UltrastarOrganizer.library().directoryOf(ti).tracks().size() > 1).toList();
        }

        if ( duet ) {
            result = result.stream().filter(TrackInfo::isDuet).toList();
        }

        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|cyan Found " + result.size() + " results: |@"));
        for (TrackInfo ti : result) {
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|green " + ti.safeName() + "|@"));
        }
    }

    private boolean suspiciousImageRatio(TrackInfo ti) {
        if ( ti.backgroundImageFile() == null || ti.coverImageFile() == null ) {
            return false;
        }

        try {
            BufferedImage cover = ImageIO.read(ti.coverImageFile());
            if (cover == null) {
                return false;
            }

            if ( Math.abs(cover.getWidth() - cover.getHeight()) < 10 ) {
                return true;
            }

            BufferedImage back = ImageIO.read(ti.backgroundImageFile());
            if (back == null) {
                return false;
            }

            if (back.getWidth() < cover.getWidth() || back.getHeight() < cover.getHeight()) {
                return true;
            }
        } catch (IOException ignored) {}

        return false;
    }
}
