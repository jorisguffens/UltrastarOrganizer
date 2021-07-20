package be.jorisg.ultrastarorganizer.commands;

import be.jorisg.ultrastarorganizer.entity.SongInfo;
import be.jorisg.ultrastarorganizer.exceptions.InvalidSongInfoFileException;
import be.jorisg.ultrastarorganizer.exceptions.LibraryException;
import be.jorisg.ultrastarorganizer.utils.Utils;
import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.EncodingAttributes;
import jdk.jshell.execution.Util;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(name = "reformat",
        description = "Reformat song directory and update info files")
public class Reformat implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", description = "The ultrastar library.")
    private File directory;

    @Override
    public Integer call() {
        File[] files = directory.listFiles();
        for (int i = 0; i < files.length; i++) {
            File songDir = files[i];
            if ( !songDir.isDirectory() ) {
                continue;
            }

            try {
                System.out.println("Processing directory " + (i + 1) + " of " +
                        files.length + ": " + songDir.getName() + "");
                process(songDir);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return 0;
    }

    private void process(File songDir) throws LibraryException, IOException {
        List<SongInfo> infoFiles = Utils.findInfoFiles(songDir);
        SongInfo main = Utils.getMainInfoFile(infoFiles);

        // find missing mp3 file
        if ( main.getMP3() == null ) {
            List<File> audioFiles = Utils.getFilesByExtensions(songDir, "mp3").stream()
                    .filter(Utils::validateMP3).collect(Collectors.toList());
            if ( audioFiles.size() > 1 ) {
                throw new LibraryException(0, "Can't select correct audio file.");
            }

            if ( !audioFiles.isEmpty() ) {
                main.setMP3(audioFiles.get(0));
            }
        }

        // find missing video file
        if ( main.getVideo() == null ) {
            List<File> videoFiles = Utils.getFilesByExtensions(songDir, Utils.VIDEO_EXT);
            if ( !videoFiles.isEmpty() ) {
                main.setVideo(videoFiles.get(0));
            }
        }

        // find missing cover file
        if ( main.getCover() == null ) {
            List<File> imageFiles = Utils.getFilesByExtensions(songDir, Utils.IMAGE_EXT).stream()
                    .filter(Utils::validateImage).collect(Collectors.toList());

            for ( File f : imageFiles ) {
                if ( main.getBackground() != null && f.equals(main.getBackground())) {
                    continue;
                }

                main.setCover(f);
                break;
            }
        }

        // find missing background file
        if ( main.getBackground() == null ) {
            List<File> imageFiles = Utils.getFilesByExtensions(songDir, Utils.IMAGE_EXT).stream()
                    .filter(Utils::validateImage).collect(Collectors.toList());

            for ( File f : imageFiles ) {
                if ( f.equals(main.getCover()) ) {
                    continue;
                }

                main.setBackground(f);
                break;
            }
        }

        // update all info files
        String filename = main.getFileName();
        for ( SongInfo si : infoFiles ) {

            // set correct filenames in info files
            si.setMP3(rename(main.getMP3(), filename));
            si.setVideo(rename(main.getVideo(), filename));
            si.setCover(rename(main.getCover(), filename + " [CO]"));
            si.setBackground(rename(main.getBackground(), filename + " [BG]"));

            // save info files
            si.save();

            String fname = si.getFileName();
            if ( si.isDuet() && !fname.toLowerCase().contains("duet") ) {
                fname += " (Duet)";
            }

            File target = new File(songDir, fname + ".txt");

            int i = 1;
            while (target.exists() && !si.getFile().equals(target)) {
                target = new File(songDir, fname + " (" + i + ").txt");
                i++;
            }
            si.renameTo(target);
        }

        // update directory filename
        if ( !songDir.getName().equals(filename) ) {
            renameDirectory(songDir, new File(songDir.getParent(), filename));
        }
    }

    private File rename(File file, String name) {
        if ( file == null ) {
            return null;
        }

        String ext = FilenameUtils.getExtension(file.getName());
        File dest = new File(file.getParent(), name + "." + ext);
        file.renameTo(dest);
        return dest;
    }

    private void renameDirectory(File source, File dest) throws IOException {
        File tmp = new File(dest.getParent(), "[TMP] " + dest.getName());
        FileUtils.copyDirectory(source, tmp);
        FileUtils.deleteDirectory(source);
        FileUtils.copyDirectory(tmp, dest);
        FileUtils.deleteDirectory(tmp);
    }
}
