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
import org.apache.commons.io.FileUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

@CommandLine.Command(name = "automatch",
        description = "Match the correct mp3 files with the correct info files.")
public class Automatch implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", description = "The ultrastar library.")
    private File directory;

    // process after matching
    private final Reformat reformat = new Reformat();

    @Override
    public Integer call() {
        File[] files = directory.listFiles();
        for (int i = 0; i < files.length; i++) {
            File songDir = files[i];
            if (!songDir.isDirectory()) {
                continue;
            }

            try {
                System.out.println("Processing directory " + (i + 1) + " of " +
                        files.length + ": " + songDir.getName() + "");
                process(songDir);
            } catch (LibraryException ex) {
                System.out.println(ex.getMessage());
                if ( !songDir.getName().startsWith("[ERROR]") ) {
                    songDir.renameTo(new File(directory, "[ERROR] - " + songDir.getName()));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return 0;
    }

    private void process(File songDir) throws LibraryException, IOException {
        SongInfo main = Utils.getMainInfoFile(songDir);
        if (main.getMP3() != null) {
            return;
        }

        File[] files = directory.listFiles();

        outer:
        for (File songFile : files) {
            if (songFile.isDirectory()) {
                continue;
            }

            String fileName = songFile.getName().toLowerCase();
            fileName = Normalizer.normalize(fileName, Normalizer.Form.NFD);

            if ( !fileName.endsWith(".mp3") ) {
                continue;
            }

            // check title
            String title = Normalizer.normalize(main.getTitle().toLowerCase(), Normalizer.Form.NFD);
            if (!fileName.contains(title)) {
                continue;
            }

            // check artists
            String[] artists = main.getArtist().toLowerCase().split(Pattern.quote(" "));
            for (String artist : artists) {
                artist = artist.replace(",", "");
                artist = artist.replace("&", "");
                artist = artist.replace(".", "");
                artist = artist.trim();

                if ( artist.equals("feat") || artist.equals("ft") ) {
                    continue;
                }

                if (!fileName.contains(artist)) {
                    continue outer;
                }
            }

            FileUtils.moveFile(songFile, new File(songDir, main.getBaseFileName() + ".mp3"));
            reformat.process(songDir);
            return;
        }

        throw new LibraryException("No mp3 file found for " + songDir.getName() + ".");
    }

}
