package be.jorisg.ultrastarorganizer.domain;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import org.apache.any23.encoding.TikaEncodingDetector;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.Normalizer;
import java.util.*;
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
        String str = safeArtist() + " - " + safeTitle();
        while (str.endsWith(".")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    public String name() {
        return artist() + " - " + title();
    }

    public Optional<String> header(String header) {
        String value = headers.get(header);
        if ( value == null || value.trim().equals("") ) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    public boolean isDuet() {
        boolean headerCheck = (header("P1").isPresent() || header("DUETSINGERP1").isPresent())
                && (header("P2").isPresent() || header("DUETSINGERP2").isPresent());
        boolean videoHeaderCheck = headers.containsKey("VIDEO") && headers.get("VIDEO").contains("p1=")
                && headers.get("VIDEO").contains("p2=");
//        boolean noteLyricsCheck = false;
//        try {
//            noteLyricsCheck = noteLyrics().noteLyricBlocks().size() > 1;
//        } catch (Exception e) {
//            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
//                    "@|red ERROR: " + safeName() + ": " + e.getMessage() + "|@"));
//        }
        return headerCheck
                || videoHeaderCheck
                || title().contains("(Duet)");
    }

    private String safe(String str) {
        str = str.replace("  ", " ");
        str = str.replace("/", "-");
        str = Normalizer.normalize(str, Normalizer.Form.NFKD); // split a character with some fancy stuff in 2 characters
        str = str.replaceAll("[^\\p{ASCII}]", ""); // remove the fancy stuff
        str = str.replaceAll("[?:]", ""); // illegal filename characters
        return str;
    }

    public String safeArtist() {
        String artist = artist();
        artist = artist.replace(",", " & ");
        artist = safe(artist);
        return artist;
    }

    public String safeTitle() {
        String title = title();
        title = title.replace(",", "");
        title = safe(title);
        return title;
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
        if (!headers.containsKey(key)) {
            return null;
        }
        String fileName = headers.get(key);
        if (fileName.trim().equals("")) {
            return null;
        }

        File file = new File(this.file.getParentFile(), fileName);
        if (file.exists() && !file.isDirectory()) {
            return file;
        }

        return null;
    }

    public File backgroundImageFile() {
        return file("BACKGROUND");
    }

    public void setBackgroundImageFileName(String name) {
        if ( name == null ) {
            headers.remove("BACKGROUND");
            return;
        }
        headers.put("BACKGROUND", name);
    }

    public File coverImageFile() {
        return file("COVER");
    }

    public void setCoverImageFileName(String name) {
        if ( name == null ) {
            headers.remove("COVER");
            return;
        }
        headers.put("COVER", name);
    }

    public File audioFile() {
        return file("MP3");
    }

    public void setAudioFileName(String name) {
        if ( name == null ) {
            headers.remove("MP3");
            return;
        }
        headers.put("MP3", name);
    }

    public File videoFile() {
        return file("VIDEO");
    }

    public void setVideoFileName(String name) {
        if ( name == null ) {
            headers.remove("VIDEO");
            return;
        }
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
        this.noteLyricLines.addAll(noteLyricCollection.toStringList());
    }

    //

    public void save() {
        try (FileWriter fw = new FileWriter(file, charset)) {
            // headers
            for (String header : headers.keySet().stream().sorted().toList()) {
                String line = "#" + header.toUpperCase() + ":" + headers.get(header.toUpperCase());

                if (header.equals("TITLE") && isDuet()) {
                    line += " (Duet)";
                }

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

    public void convertDuetFormat() {
        NoteLyricCollection nlc = noteLyrics();

        if (headers.containsKey("DUETSINGERP1")) {
            headers.put("DUETSINGERP1", "P1");
        }
        if (headers.containsKey("DUETSINGERP2")) {
            headers.put("DUETSINGERP2", "P2");
        }

        noteLyricLines.clear();
        noteLyricLines.addAll(nlc.toStringList());
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
        int i = 0;
        for ( ; i < contents.size(); i++) {
            String line = contents.get(i);
            if (!line.startsWith("#")) {
                break;
            }

            String key = line.split(Pattern.quote(":"))[0].substring(1);
            String value = line.substring(key.length() + 2).trim();
            headers.put(key.toUpperCase(), value);
        }

        List<String> noteLyricLines = contents.subList(i, contents.size());

        if (headers.size() == 0 || !headers.containsKey("ARTIST") || !headers.containsKey("TITLE")) {
            throw new RuntimeException("Required headers are missing from file.");
        }

        return new TrackInfo(file, charset, headers, noteLyricLines);
    }

}
