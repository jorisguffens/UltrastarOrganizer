package be.jorisg.ultrastarorganizer.organizer;

import be.jorisg.ultrastarorganizer.exceptions.InvalidSongInfoFileException;
import be.jorisg.ultrastarorganizer.entity.SongFiles;
import be.jorisg.ultrastarorganizer.entity.SongInfo;
import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.EncodingAttributes;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LibraryOrganizer {

    private final File directory;

    public LibraryOrganizer(File directory) {
        if ( !directory.isDirectory() ) {
            throw new IllegalArgumentException("File is not a directory.");
        }
        this.directory = directory;
    }

    public void run(boolean convertAudio, boolean removeVideo, boolean cleanCaches) {
        File[] files = directory.listFiles();
        for ( int i = 0; i < files.length; i++ ) {
            try {
                File songDir = files[i];
                System.out.println("Processing directory " + (i+1) + " of " + files.length + ": " + songDir.getName() + "");
                process(songDir, convertAudio, removeVideo, cleanCaches);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void process(File dir, boolean convertAudio, boolean removeVideo, boolean cleanCaches) throws IOException {
        List<File> txtFiles = getFilesByExtensions(dir, "txt");
        if ( txtFiles.isEmpty() ) {
            System.out.println("No song info (.txt) found.");
            errorDirectory(dir, 0);
            return;
        }

        List<SongInfo> songInfos = new ArrayList<>();
        for ( File file : txtFiles ) {
            try {
                SongInfo info = new SongInfo(file);
                songInfos.add(info);
            } catch (InvalidSongInfoFileException e) {}
        }

        if ( songInfos.isEmpty() ) {
            System.out.println("No valid song info files (.txt) found.");
            errorDirectory(dir, 1);
            return;
        }

        SongInfo mainInfo = songInfos.stream()
                .filter(s -> !s.isDuet())
                .findFirst()
                .orElse(songInfos.get(0));

        File mp3 = null;
        File video = null;
        File background = null;
        File cover = null;

        // mp3
        if ( mainInfo.getMP3() != null ) {
            mp3 = mainInfo.getMP3();
        } else {
            List<File> audioFiles = getFilesByExtensions(dir, "mp3").stream().filter(this::validateMP3).collect(Collectors.toList());
            if ( audioFiles.size() > 1 ) {
                System.out.println("Can't select correct audio file.");
                errorDirectory(dir, 2);
                return;
            }

            if ( !audioFiles.isEmpty() ) {
                mp3 = audioFiles.get(0);
            }
        }

        // video
        List<File> videoFiles = getFilesByExtensions(dir, "mp4", "avi", "mkv", "flv", "mov", "mpg");
        if ( mainInfo.getVideo() != null ) {
            video = mainInfo.getVideo();
        } else {
            if ( !videoFiles.isEmpty() ) {
                video = videoFiles.get(0);
            }
        }

        // cover
        List<File> imageFiles = getFilesByExtensions(dir, "jpg", "png").stream().filter(this::validateImage).collect(Collectors.toList());
        if ( mainInfo.getCover() != null ) {
            cover = mainInfo.getCover();
        } else {
            for ( File f : imageFiles ) {
                if ( mainInfo.getBackground() == null || !f.equals(mainInfo.getBackground())) {
                    background = f;
                    break;
                }
            }
        }

        // background
        if ( mainInfo.getBackground() != null ) {
            background = mainInfo.getBackground();
        } else {
            for ( File f : imageFiles ) {
                if (!f.equals(cover)) {
                    background = f;
                    break;
                }
            }
        }

        String filename = mainInfo.getFileName();

        if ( mp3 == null ) {
            if ( !convertAudio ) {
                System.out.println("No audio file found.");
                errorDirectory(dir, 3);
                return;
            }

            if ( videoFiles.isEmpty() ) {
                System.out.println("No audio and video file found.");
                errorDirectory(dir, 3);
                return;
            }

            System.out.println("Converting video file to mp3...");
            File source = videoFiles.get(0);
            File target = new File(dir, filename + ".mp3");
            convertVideoToMP3(source, target);
            if ( target.exists() ) {
                mp3 = target;
            }
        }

        if ( removeVideo && video != null) {
            video.delete();
        }

        if ( cleanCaches ) {
            List<File> cacheFiles = getFilesByExtensions(dir, "db", "sco", "ini");
            for ( File file : cacheFiles ) {
                file.delete();
            }
            File wdmc = new File(dir, ".wdmc");
            if ( wdmc.exists() ) {
                FileUtils.deleteDirectory(wdmc);
            }
        }

        SongFiles songFiles = new SongFiles(mp3, video, background, cover);
        songFiles.renameAll(filename);

        for ( SongInfo info : songInfos ) {
            songFiles.apply(info);
            info.save();

            String fname = info.getFileName();
            if ( info.isDuet() && !fname.toLowerCase().contains("duet") ) {
                fname += " (Duet)";
            }

            File target = new File(dir, fname + ".txt");

            int i = 1;
            while ( target.exists() && !info.getFile().equals(target) ) {
                target = new File(dir, fname + " (" + i + ").txt");
                i++;
            }
            info.renameTo(target);
        }

        if ( !dir.getName().equals(filename) ) {
            renameDirectory(dir, new File(dir.getParent(), filename));
        }
    }

    private List<File> getFilesByExtensions(File directory, String... extensions) {
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

    private void errorDirectory(File songDir, int code) throws IOException {
        renameDirectory(songDir, new File(songDir.getParent(), "[ERR" + code + "] "
                + songDir.getName().replaceFirst("(\\[ERR[0-9]*\\])", "").trim()));
    }

    private void renameDirectory(File source, File dest) throws IOException {
        File tmp = new File(dest.getParent(), "[TMP] " + dest.getName());
        FileUtils.copyDirectory(source, tmp);
        FileUtils.deleteDirectory(source);
        FileUtils.copyDirectory(tmp, dest);
        FileUtils.deleteDirectory(tmp);
    }

    private void convertVideoToMP3(File source, File target) {
        AudioAttributes audio = new AudioAttributes();
        audio.setCodec("libmp3lame");
        audio.setBitRate(192000);
        audio.setChannels(2);
        audio.setSamplingRate(44100);

        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setFormat("mp3");
        attrs.setAudioAttributes(audio);

        Encoder encoder = new Encoder();
        try {
            encoder.encode(source, target, attrs);
        } catch (IllegalArgumentException | EncoderException e) {
            e.printStackTrace();
        }
    }

    private final Tika tika = new Tika();

    private boolean validateMP3(File file) {
        try {
            String mediaType = tika.detect(file);
            return mediaType.equals("media/mp3") || mediaType.equals("audio/mpeg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean validateImage(File file) {
        try {
            String mediaType = tika.detect(file);
            return mediaType.equals("image/png") || mediaType.equals("image/jpeg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}
