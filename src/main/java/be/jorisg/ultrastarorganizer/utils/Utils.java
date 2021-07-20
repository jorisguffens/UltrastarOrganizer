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
        return getMainInfoFile(findInfoFiles(songDir));
    }

    public static SongInfo getMainInfoFile(List<SongInfo> infoFiles) {
        return infoFiles.stream().filter(s -> !s.isDuet()).findFirst()
                .orElseGet(() -> infoFiles.get(0));
    }

    public static List<SongInfo> findInfoFiles(File songDir) throws LibraryException {
        List<File> txtFiles = Utils.getFilesByExtensions(songDir, "txt");
        if ( txtFiles.isEmpty() ) {
            throw new LibraryException(0, "No song info (.txt) found.");
        }

        List<SongInfo> infos = new ArrayList<>();
        for ( File file : txtFiles ) {
            try {
                SongInfo info = new SongInfo(file);
                infos.add(info);
            } catch (InvalidSongInfoFileException ignored) {}
        }

        if ( infos.isEmpty() ) {
            throw new LibraryException(1, "No valid song info files (.txt) found.");
        }

        return infos;
    }

}
