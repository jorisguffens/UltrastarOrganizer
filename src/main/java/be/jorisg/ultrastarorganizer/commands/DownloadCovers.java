/*
 * This file is part of Ultrastar Organizer, licensed under the MIT License.
 *
 * Copyright (c) Joris Guffens
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package be.jorisg.ultrastarorganizer.commands;

import be.jorisg.ultrastarorganizer.entity.SongInfo;
import be.jorisg.ultrastarorganizer.exceptions.LibraryException;
import be.jorisg.ultrastarorganizer.utils.Utils;
import org.apache.hc.core5.http.ParseException;
import picocli.CommandLine;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Track;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "downloadcovers",
        description = "Download missing cover files.")
public class DownloadCovers implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", description = "The ultrastar library.")
    private File directory;

    @CommandLine.Parameters(index = "1", description = "The ultrastar library.")
    private String spotifyClientId;

    @CommandLine.Parameters(index = "2", description = "The ultrastar library.")
    private String spotifyClientSecret;

    private SpotifyApi spotifyApi;

    @Override
    public Integer call() {
        // login into spotify api
        try {
            spotifyApi = new SpotifyApi.Builder()
                    .setClientId(spotifyClientId)
                    .setClientSecret(spotifyClientSecret)
                    .build();

            String token = spotifyApi.clientCredentials().build().execute().getAccessToken();
            spotifyApi.setAccessToken(token);
        } catch (IOException | ParseException | SpotifyWebApiException e) {
            throw new RuntimeException(e);
        }

        for (File songDir : directory.listFiles()) {
            if (!songDir.isDirectory()) {
                continue;
            }

            try {
                System.out.println("");
                process(songDir);
                return 0;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return 0;
    }

    private void process(File songDir) throws LibraryException, IOException, ParseException, SpotifyWebApiException {
        SongInfo main = Utils.getMainInfoFile(songDir);
        if (main.getCover() != null && main.getCover().exists()) {
            // cover file exists
            BufferedImage img = ImageIO.read(main.getCover());
            if (img != null) {
                return; // cover can be loaded
            }
        }

        System.out.println("Looking up " + songDir.getName() + "...");

//        // wait for rate limit
//        long diff = Instant.now().toEpochMilli() - previousDownload.toEpochMilli();
//        if (diff < 1000) {
//            Thread.sleep(diff + 100);
//        }
//        previousDownload = Instant.now();

        // get work for given title and artist
        Track[] tracks = spotifyApi.searchTracks(main.getTitle() + " - " + main.getArtist())
                .limit(1).build().execute().getItems();

        if (tracks.length == 0) {
            System.out.println("No tracks found.");
            return;
        }

        Image[] images = tracks[0].getAlbum().getImages();
        Image image = Arrays.stream(images).max(Comparator.comparingInt(img -> img.getWidth() * img.getHeight()))
                .orElse(null);
        if (image == null) {
            System.out.println("No album image found.");
            return;
        }

        BufferedImage img = ImageIO.read(new URL(image.getUrl()));
        File file = new File(songDir, main.getBaseFileName() + " [CO].png");
        ImageIO.write(img, "png", file);

        main.setCover(file);
        main.save();
    }

}
