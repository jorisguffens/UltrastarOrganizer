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
