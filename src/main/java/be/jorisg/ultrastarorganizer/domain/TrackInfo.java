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

package be.jorisg.ultrastarorganizer.domain;

import org.apache.any23.encoding.TikaEncodingDetector;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TrackInfo {

    private final static TikaEncodingDetector tika = new TikaEncodingDetector();

    public File infoFile;

    private final Charset charset;
    private final Map<String, String> headers;
    private final List<String> noteLyricLines;

    public TrackInfo(Charset charset, Map<String, String> headers, List<String> noteLyricLines) {
        this.charset = charset;
        this.headers = headers;
        this.noteLyricLines = noteLyricLines;
    }

    public String asciiName() {
        String name = artist().replace(",", " & ") + " - " + title();
        name = name.replace("  ", " ");
        name = name.replace("/", "-");
        name = Normalizer.normalize(name, Normalizer.Form.NFKD); // split a character with some fancy stuff in 2 characters
        name = name.replaceAll("[^\\p{ASCII}]", ""); // remove the fancy stuff
        name = name.replaceAll("[?]|[.]$", ""); // illegal filename characters
        return name;
    }

    public String name() {
        return artist() + " - " + title();
    }

    public boolean isDuet() {
        return headers.containsKey("DUETSINGERP1") && !headers.get("DUETSINGERP1").equals("")
                && headers.containsKey("DUETSINGERP2") && !headers.get("DUETSINGERP2").equals("");
    }

    public String title() {
        return headers.get("TITLE");
    }

    public void setTitle(String title) {
        headers.put("TITLE", title);
    }

    public String artist() {
        return headers.get("ARTIST");
    }

    public void setArtist(String artist) {
        headers.put("ARTIST", artist);
    }

    private Path path(String name) {
        if (headers.containsKey(name) ) {
            return Path.of(headers.get(name));
        }
        return null;
    }

    public Path backgroundImagePath() {
        return path("BACKGROUND");
    }

    public void setBackgroundImagePath(Path path) {
        headers.put("BACKGROUND", path.toString());
    }

    public Path coverImagePath() {
        return path("COVER");
    }

    public void setCoverImagePath(Path path) {
        headers.put("COVER", path.toString());
    }

    public Path audioPath() {
        return path("MP3");
    }

    public void setAudioPath(Path path) {
        headers.put("MP3", path.toString());
    }

    public Path videoPath() {
        return path("VIDEO");
    }

    public void setVideo(Path path) {
        headers.put("VIDEO", path.toString());
    }

    //

    public void save(File file) {
        try (FileOutputStream outputStream = new FileOutputStream(infoFile) ) {

            // headers
            for ( String header : headers.keySet().stream().sorted().toList()) {
                String line = "#" + header.toUpperCase() + ":" + headers.get(header.toUpperCase());
                outputStream.write((line + "\n").getBytes(charset));
            }

            // notes
            for ( String line : noteLyricLines ) {
                outputStream.write((line + "\n").getBytes(charset));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static TrackInfo load(File file) {
        Charset charset;
        try (FileInputStream fis = new FileInputStream(file)) {
            charset = Charset.forName(tika.guessEncoding(fis));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<String> contents;
        try {
            contents = IOUtils.readLines(file.toURI().toURL().openStream(), charset);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<String, String> headers = new HashMap<>();
        for (String line : contents) {
            if (!line.startsWith("#")) {
                break;
            }

            String key = line.split(Pattern.quote(":"))[0].substring(1);
            String value = line.substring(key.length() + 2).trim();
            headers.put(key.toUpperCase(), value);
        }

        List<String> noteLyricLines = contents.subList(headers.size(), contents.size());

        if ( headers.size() == 0 || !headers.containsKey("ARTIST") || !headers.containsKey("TITLE") ) {
            throw new RuntimeException("Required headers are missing from file.");
        }

        return new TrackInfo(charset, headers, noteLyricLines);
    }

}
