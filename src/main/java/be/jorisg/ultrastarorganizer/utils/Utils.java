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

package be.jorisg.ultrastarorganizer.utils;

import be.jorisg.ultrastarorganizer.entity.SongInfo;
import be.jorisg.ultrastarorganizer.exceptions.InvalidSongInfoFileException;
import be.jorisg.ultrastarorganizer.exceptions.LibraryException;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    public final static String[] VIDEO_EXT = new String[] { "mp4", "avi", "mkv", "flv", "mov", "mpg", "m4v", "divx" };
    public final static String[] IMAGE_EXT = new String[] { "jpg", "png", "jpeg" };

    private final static Tika tika = new Tika();

    public static List<File> getFilesByExtensions(File directory, String... extensions) {
        List<File> result = new ArrayList<>();
        for ( File file : directory.listFiles() ) {
            String ext = FilenameUtils.getExtension(file.getName());
            for ( String testExt : extensions ) {
                if ( ext.equalsIgnoreCase(testExt) ) {
                    result.add(file);
                    break;
                }
            }
        }
        return result;
    }

    public static boolean validateMP3(File file) {
        try {
            String mediaType = tika.detect(file);
            return mediaType.equals("media/mp3") || mediaType.equals("audio/mpeg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean validateImage(File file) {
        try {
            String mediaType = tika.detect(file);
            return mediaType.equals("image/png") || mediaType.equals("image/jpeg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static SongInfo getMainInfoFile(File songDir) throws LibraryException {
        return getMainInfoFile(getInfoFiles(songDir));
    }

    public static SongInfo getMainInfoFile(List<SongInfo> infoFiles) {
        return infoFiles.stream().filter(s -> !s.isDuet()).findFirst()
                .orElseGet(() -> infoFiles.get(0));
    }

    public static List<SongInfo> getInfoFiles(File songDir) throws LibraryException {
        List<File> txtFiles = Utils.getFilesByExtensions(songDir, "txt");
        if ( txtFiles.isEmpty() ) {
            throw new LibraryException("No song info (.txt) found.");
        }

        List<SongInfo> infos = new ArrayList<>();
        for ( File file : txtFiles ) {
            try {
                SongInfo info = new SongInfo(file);
                infos.add(info);
            } catch (InvalidSongInfoFileException ex) {
                System.out.println(ex.getMessage());
            }
        }

        if ( infos.isEmpty() ) {
            throw new LibraryException("No valid song info files (.txt) found.");
        }

        return infos;
    }

}
