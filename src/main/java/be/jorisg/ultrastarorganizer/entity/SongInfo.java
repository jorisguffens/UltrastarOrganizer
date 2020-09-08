package be.jorisg.ultrastarorganizer.entity;

import be.jorisg.ultrastarorganizer.exceptions.InvalidSongInfoFileException;
import org.apache.any23.encoding.TikaEncodingDetector;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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

    public File infoFile;

    private final Map<String, String> headers = new HashMap<>();
    private List<String> notes = new ArrayList<>();

    public SongInfo(File infoFile) throws InvalidSongInfoFileException {
        this.infoFile = infoFile;

        Charset charset;
        try (FileInputStream fis = new FileInputStream(infoFile)) {
            charset = Charset.forName(new TikaEncodingDetector().guessEncoding(fis));
        } catch (IOException e) {
            throw new InvalidSongInfoFileException(e);
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(infoFile.toPath(), charset);
        } catch (IOException e) {
            throw new InvalidSongInfoFileException(e);
        }

        for ( int i = 0; i < lines.size(); i++ ) {
            String line = lines.get(i);
            line = Normalizer.normalize(line, Normalizer.Form.NFD);
            line = line.replaceAll("[^\\p{ASCII}]", "");
            lines.set(i, line);
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
        return f.exists() && f.isFile() && f.exists() ? f : null;
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

    public String getFileName() {
        String name = getArtist() + " - " + getTitle();
        name = name.replace("/", "-");
        name = name.replaceAll("[^\\p{ASCII}]", "");
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
                outputStream.write((line + "\n").getBytes(StandardCharsets.US_ASCII));
            }

            // notes
            for ( String line : notes ) {
                outputStream.write((line + "\n").getBytes(StandardCharsets.US_ASCII));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    public void save() {
        List<String> lines;
        try {
            lines = Files.readAllLines(file.toPath(), StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        for ( int i = 0; i < lines.size(); i++ ) {
            String line = lines.get(i);
            if ( !line.startsWith("#") ) {
                break;
            }

            String key = line.split(Pattern.quote(":"))[0].substring(1);
            String value = line.substring(key.length() + 2);

            if ( key.equals("ARTIST") ) {
                value = artist;
            } else if ( key.equals("TITLE") ) {
                value = title;
            } else if ( key.equals("BACKGROUND") ) {
                value = background == null ? "" : background;
            } else if ( key.equals("COVER") ) {
                value = cover == null ? "" : cover;
            } else if ( key.equals("MP3") ) {
                value = mp3 == null ? "" : mp3;
            } else if ( key.equals("VIDEO") ) {
                value = video == null ? "" : video;
            }

            line = "#" + key + ":" + value;
            lines.set(i, line);
        }

        saveLines(lines);
    }

    private void saveLines(List<String> lines) {
        try (FileOutputStream outputStream = new FileOutputStream(file) ) {
            for (String line : lines) {
                outputStream.write((line + "\n").getBytes(StandardCharsets.US_ASCII));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    */

}
