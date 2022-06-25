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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TrackInfo {

    private final static TikaEncodingDetector tika = new TikaEncodingDetector();

    private File file;

    private final Charset charset;
    private final Map<String, String> headers;
    private final List<String> noteLyricLines;

    private TrackInfo(File file, Charset charset, Map<String, String> headers, List<String> noteLyricLines) {
        this.file = file;
        this.charset = charset;
        this.headers = headers;
        this.noteLyricLines = noteLyricLines;
    }

    public File file() {
        return file;
    }

    public File parentDirectory() {
        return file.getParentFile();
    }

    public String safeName() {
        String name = artist().replace(",", " & ") + " - " + title().replace(",", "");
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

    public String[] artists() {
        return Arrays.stream(artist().split("(?i)([,]|(feat\\.)|(ft\\.))"))
                .map(String::trim).toArray(String[]::new);
    }

    public void setArtist(String artist) {
        headers.put("ARTIST", artist);
    }

    private File file(String key) {
        if ( !headers.containsKey(key) ) {
            return null;
        }
        String fileName = headers.get(key);
        if ( fileName.trim().equals("") ) {
            return null;
        }

        File file = new File(this.file.getParentFile(), fileName);
        if ( file.exists() && !file.isDirectory() ) {
            return file;
        }

        return null;
    }

    public File backgroundImageFile() {
        return file("BACKGROUND");
    }

    public void setBackgroundImageFileName(String name) {
        headers.put("BACKGROUND", name);
    }

    public File coverImageFile() {
        return file("COVER");
    }

    public void setCoverImageFileName(String name) {
        headers.put("COVER", name);
    }

    public File audioFile() {
        return file("MP3");
    }

    public void setAudioFileName(String name) {
        headers.put("MP3", name);
    }

    public File videoFile() {
        return file("VIDEO");
    }

    public void setVideoFileName(String name) {
        headers.put("VIDEO", name);
    }

    public void moveTo(File file) throws IOException {
        FileUtils.moveFile(this.file, file);
        this.file = file;
    }

    public NoteLyricCollection noteLyrics() {
        return NoteLyricCollection.fromStringList(noteLyricLines);
    }

    public void setNoteLyrics(NoteLyricCollection noteLyricCollection) {
        this.noteLyricLines.clear();
        this. noteLyricLines.addAll(noteLyricCollection.toStringList());
    }

    //

    public void save() {
        try (FileWriter fw = new FileWriter(file, charset)) {

            // headers
            for (String header : headers.keySet().stream().sorted().toList()) {
                String line = "#" + header.toUpperCase() + ":" + headers.get(header.toUpperCase());
                fw.write(line + "\n");
            }

            // notes
            for (String line : noteLyricLines) {
                fw.write(line + "\n");
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

        if (headers.size() == 0 || !headers.containsKey("ARTIST") || !headers.containsKey("TITLE")) {
            throw new RuntimeException("Required headers are missing from file.");
        }

        return new TrackInfo(file, charset, headers, noteLyricLines);
    }

}
