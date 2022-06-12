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

package be.jorisg.ultrastarorganizer.entity;

import be.jorisg.ultrastarorganizer.exceptions.InvalidSongInfoFileException;
import org.apache.any23.encoding.TikaEncodingDetector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SongInfo {

    private final static TikaEncodingDetector tika = new TikaEncodingDetector();

    public File infoFile;

    private final Map<String, String> headers = new HashMap<>();
    private List<String> notes = new ArrayList<>();

    private final Charset charset;

    public SongInfo(File infoFile) throws InvalidSongInfoFileException {
        this.infoFile = infoFile;

        try (FileInputStream fis = new FileInputStream(infoFile)) {
            charset = Charset.forName(tika.guessEncoding(fis));
        } catch (IOException e) {
            throw new InvalidSongInfoFileException(e);
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(infoFile.toPath(), charset);
        } catch (IOException e) {
            throw new InvalidSongInfoFileException(e);
        }

        int n;
        for ( n = 0; n < lines.size(); n++ ) {
            String line = lines.get(n);
            if (!line.startsWith("#")) {
                break;
            }

            String key = line.split(Pattern.quote(":"))[0].substring(1);
            String value = line.substring(key.length() + 2).trim();
            headers.put(key.toUpperCase(), value);
        }

        for ( int i = n; i < lines.size(); i++ ) {
            notes.add(lines.get(i));
        }

        if ( headers.size() == 0 || !containsHeader("artist") || !containsHeader("title") ) {
            throw new InvalidSongInfoFileException("Missing headers.");
        }
    }

    public File getFile() {
        return infoFile;
    }

    public String getName() {
        return infoFile.getName();
    }

    public List<String> getNotes() {
        return notes;
    }

    public void setNotes(List<String> notes) {
        this.notes = notes;
    }

    public boolean containsHeader(String key) {
        return headers.containsKey(key.toUpperCase());
    }

    public String getHeaderValue(String key) {
        return headers.get(key.toUpperCase());
    }

    public void setHeaderValue(String key, String value) {
        headers.put(key.toUpperCase(), value);
    }

    private File getHeaderFile(String key) {
        String value = getHeaderValue(key);
        if ( value == null || value.equals("") ) {
            return null;
        }
        File f = new File(infoFile.getParent(), value);
        return f.exists() && f.isFile() ? f : null;
    }

    public boolean isDuet() {
        return containsHeader("DUETSINGERP1") && !getHeaderValue("DUETSINGERP1").equals("")
                && containsHeader("DUETSINGERP2") && !getHeaderValue("DUETSINGERP2").equals("");
    }

    public String getTitle() {
        return getHeaderValue("title");
    }

    public void setTitle(String title) {
        setHeaderValue("title", title);
    }

    public String getArtist() {
        return getHeaderValue("artist");
    }

    public void setArtist(String artist) {
        setHeaderValue("artist", artist);
    }

    public File getBackground() {
        return getHeaderFile("background");
    }

    public void setBackground(File file) {
        setHeaderValue("background", getRelativePath(file));
    }

    public File getCover() {
        return getHeaderFile("cover");
    }

    public void setCover(File file) {
        setHeaderValue("cover", getRelativePath(file));
    }

    public File getMP3() {
        return getHeaderFile("mp3");
    }

    public void setMP3(File file) {
        setHeaderValue("mp3", getRelativePath(file));
    }

    public File getVideo() {
        return getHeaderFile("video");
    }

    public void setVideo(File file) {
        setHeaderValue("video", getRelativePath(file));
    }

    private String getRelativePath(File file) {
        return file == null ? "" : Paths.get(infoFile.getParent()).relativize(Paths.get(file.toURI())).toString();
    }

    public String getBaseFileName() {
        String name = getArtist().replace(",", " & ").replace("  ", " ")
                + " - " + getTitle();
        name = name.replace("/", "-");
        name = Normalizer.normalize(name, Normalizer.Form.NFKD); // split a character with some fancy stuff in 2 characters
        name = name.replaceAll("[^\\p{ASCII}]", ""); // remove the fancy stuff
        name = name.replaceAll("[?]|[.]$", ""); // illegal filename characters
        return name;
    }

    public void renameTo(File target) {
        if ( infoFile.renameTo(target) ) {
            infoFile = target;
        }
    }

    public void save() {
        try (FileOutputStream outputStream = new FileOutputStream(infoFile) ) {

            // headers
            for ( String header : headers.keySet().stream().sorted().collect(Collectors.toList()) ) {
                String line = "#" + header.toUpperCase() + ":" + headers.get(header.toUpperCase());
                outputStream.write((line + "\n").getBytes(charset));
            }

            // notes
            for ( String line : notes ) {
                outputStream.write((line + "\n").getBytes(charset));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
