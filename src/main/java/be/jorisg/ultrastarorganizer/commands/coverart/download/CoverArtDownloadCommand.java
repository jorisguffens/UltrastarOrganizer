package be.jorisg.ultrastarorganizer.commands.coverart.download;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import be.jorisg.ultrastarorganizer.domain.Library;
import be.jorisg.ultrastarorganizer.domain.TrackDirectory;
import picocli.CommandLine;

import java.util.regex.Pattern;

@CommandLine.Command(
        name = "download",
        description = "Download missing cover art image files."
)
public class CoverArtDownloadCommand implements Runnable {

    @CommandLine.Option(names = {"--spotify"}, description = "Use the spotify API to download missing cover art. You must provide a clientId and clientSecret.")
    private String spotifySecret;

    @CommandLine.Option(names = {"--market"}, description = "A two-letter ISO 3166-1 country code.")
    private String market = null;

    @Override
    public void run() {
        if ( spotifySecret != null ) {
            if ( !spotifySecret.contains(":") ) {
                UltrastarOrganizer.out.println("You must provide a clientId and clientSecret separated by a colon.");
                return;
            }

            String[] clientCredentials = spotifySecret.split(Pattern.quote(":"));
            try {
                SpotifyDownloader sd = new SpotifyDownloader(clientCredentials[0], clientCredentials[1], market);
                Library library = UltrastarOrganizer.refresh();
                for (TrackDirectory td : library.trackDirectories()) {
                    process(td, sd);
                }
            } catch (Exception e) {
                UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|red ERROR:|@ " + e.getMessage()));
            }
            return;
        }

        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|red ERROR: No valid download strategy provided.|@"));
    }

    public void process(TrackDirectory td, SpotifyDownloader sd) {
        try {
            sd.process(td);
        } catch (Exception e) {
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|red ERROR: " + e.getMessage() + "|@"));
        }
    }

}
