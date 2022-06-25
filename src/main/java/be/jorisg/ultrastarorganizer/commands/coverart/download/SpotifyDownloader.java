package be.jorisg.ultrastarorganizer.commands.coverart.download;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import be.jorisg.ultrastarorganizer.domain.TrackDirectory;
import be.jorisg.ultrastarorganizer.domain.TrackInfo;
import com.neovisionaries.i18n.CountryCode;
import org.apache.hc.core5.http.ParseException;
import picocli.CommandLine;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.data.search.simplified.SearchTracksRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;

public class SpotifyDownloader {

    private final SpotifyApi spotifyApi;
    private final String market;

    SpotifyDownloader(String clientId, String clientSecret, String market) throws Exception {
        spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .build();

        String token = spotifyApi.clientCredentials().build().execute().getAccessToken();
        spotifyApi.setAccessToken(token);
        this.market = market;
    }

    SpotifyDownloader(String clientId, String clientSecret) throws Exception {
        this(clientId, clientSecret, null);
    }

    public void process(TrackDirectory td) throws IOException, ParseException, SpotifyWebApiException {
        TrackInfo main = td.originalTrack();

        if (main.coverImageFile() != null) {
            // if a file exists, check if it is valid.
            BufferedImage img = ImageIO.read(main.coverImageFile());
            if (img != null) {
                return; // cover can be loaded
            }
        }

        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|cyan Downloading cover for " + main.name() + "|@"));

        String artist = String.join(" & ", main.artists());
        String title = main.title().replace(" (Duet)", "");
        String query = String.format("artist:%s track:%s", artist, title);

        SearchTracksRequest.Builder b = spotifyApi.searchTracks(query).limit(1);
        if ( market != null ) {
            b = b.market(CountryCode.getByCode(market));
        }

        SearchTracksRequest str = b.build();
        Track[] tracks = str.execute().getItems();

        if (tracks.length == 0) {
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|red \tERROR: No results found.|@"));
            return;
        }

        Image[] images = tracks[0].getAlbum().getImages();
        Image image = Arrays.stream(images).max(Comparator.comparingInt(img -> img.getWidth() * img.getHeight()))
                .orElse(null);
        if (image == null) {
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|red \tERROR: No album image for result.|@"));
            return;
        }

        BufferedImage img = ImageIO.read(new URL(image.getUrl()));
        File file = new File(td.directory(), main.safeName() + " [CO].png");
        ImageIO.write(img, "png", file);

        main.setCoverImageFileName(file.getName());
        main.save();
    }

}
