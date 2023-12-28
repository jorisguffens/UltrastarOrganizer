package be.jorisg.ultrastarorganizer.domain;

import com.ibm.icu.text.CharsetDetector;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

public class TrackInfo {

    private File file;

    private final Map<String, String> headers;
    private List<String> noteLyricLines;

    private TrackInfo(File file, Map<String, String> headers, List<String> noteLyricLines) {
        this.file = file;
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
        if (value == null || value.trim().equals("")) {
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
        return headerCheck || videoHeaderCheck;
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
        artist = artist.replace("*", "");
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
        if (name == null) {
            headers.remove("BACKGROUND");
            return;
        }
        headers.put("BACKGROUND", name);
    }

    public File coverImageFile() {
        return file("COVER");
    }

    public void setCoverImageFileName(String name) {
        if (name == null) {
            headers.remove("COVER");
            return;
        }
        headers.put("COVER", name);
    }

    public File audioFile() {
        return file("MP3");
    }

    public void setAudioFileName(String name) {
        if (name == null) {
            headers.remove("MP3");
            return;
        }
        headers.put("MP3", name);
    }

    public File videoFile() {
        return file("VIDEO");
    }

    public void setVideoFileName(String name) {
        if (name == null) {
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
        return NoteLyricCollection.fromStringList(noteLyricLines, isNoteLyricsRelative());
    }

    public void overwriteNoteLyrics(NoteLyricCollection noteLyricCollection) {
        this.noteLyricLines = noteLyricCollection.toStringList();
    }

    private boolean isNoteLyricsRelative() {
        return headers.containsKey("RELATIVE") && headers.get("RELATIVE").equalsIgnoreCase("YES");
    }

    //

    public void save() {
        try (FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8)) {
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
        CharsetDetector detector = new CharsetDetector();

        Charset charset;
        try (FileInputStream fis = new FileInputStream(file)) {
            detector.setText(fis.readAllBytes());
            charset = Charset.forName(detector.detect().getName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<String> contents;
        try {
            ByteBuffer bb = ByteBuffer.wrap(FileUtils.readFileToByteArray(file));
            CharBuffer cb = charset.decode(bb);
            bb = StandardCharsets.UTF_8.encode(cb);
            contents = new String(bb.array(), StandardCharsets.UTF_8).lines().toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<String, String> headers = new HashMap<>();
        int i = 0;
        for (; i < contents.size(); i++) {
            String line = contents.get(i);
            if (!line.startsWith("#")) {
                break;
            }

            String key = line.split(Pattern.quote(":"))[0].substring(1);
            String value = line.substring(key.length() + 2).trim();
            if (value.isBlank()) {
                continue;
            }

            headers.put(key.toUpperCase(), value);
        }

        List<String> noteLyricLines = contents.subList(i, contents.size());

        if (headers.isEmpty() || !headers.containsKey("ARTIST") || !headers.containsKey("TITLE")) {
            throw new IllegalArgumentException("Required headers are missing from file.");
        }

        if (headers.containsKey("DUETSINGERP1")) {
            headers.put("P1", headers.get("DUETSINGERP1"));
            headers.remove("DUETSINGERP1");
        }
        if (headers.containsKey("DUETSINGERP2")) {
            headers.put("P2", headers.get("DUETSINGERP2"));
            headers.remove("DUETSINGERP2");
        }

        if (headers.containsKey("RELATIVE") && headers.get("RELATIVE").equalsIgnoreCase("YES")
                && (!headers.containsKey("CALCMEDLEY") || !headers.get("CALCMEDLEY").equalsIgnoreCase("OFF"))
                && !headers.containsKey("MEDLEYSTARTBEAT") && !headers.containsKey("MEDLEYENDBEAT")) {
            throw new IllegalArgumentException("Medley calculation must be disabled when using relative notes.");
        }

        return new TrackInfo(file, headers, noteLyricLines);
    }

}
