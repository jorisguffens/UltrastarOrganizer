package be.jorisg.ultrastarorganizer.synchronizer;

import be.jorisg.ultrastarorganizer.exceptions.InvalidSongInfoFileException;
import be.jorisg.ultrastarorganizer.entity.SongInfo;
import be.jorisg.ultrastarorganizer.utils.Utils;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LibrarySynchronizer {

    private final File directory;

    public LibrarySynchronizer(File directory) {
        if ( !directory.isDirectory() ) {
            throw new IllegalArgumentException("File is not a directory.");
        }
        this.directory = directory;
    }

    public void run(boolean update) {
        File[] files = directory.listFiles();
        for ( int i = 0; i < files.length; i++ ) {
            try {
                File songDir = files[i];
                System.out.println("Processing directory " + (i+1) + " of " + files.length + ": " + songDir.getName() + "");
                process(songDir, update);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("Done!");
    }

    private void process(File dir, boolean update) {
        List<File> txtFiles = Utils.getFilesByExtensions(dir, "txt");
        if ( txtFiles.isEmpty() ) {
            System.out.println("No song info (.txt) found.");
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
            return;
        }

        File audioFile = songInfos.get(0).getMP3();

    }

}
