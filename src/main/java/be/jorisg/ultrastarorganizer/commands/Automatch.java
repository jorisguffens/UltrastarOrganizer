package be.jorisg.ultrastarorganizer.commands;

import be.jorisg.ultrastarorganizer.entity.SongInfo;
import be.jorisg.ultrastarorganizer.exceptions.LibraryException;
import be.jorisg.ultrastarorganizer.utils.Utils;
import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.EncodingAttributes;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@CommandLine.Command(name = "automatch",
        description = "Match the correct mp3 files with the correct info files.")
public class Automatch implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", description = "The ultrastar library.")
    private File directory;

    // process after matching
    private final Reformat reformat = new Reformat();

    @Override
    public Integer call() {
        File[] files = directory.listFiles();
        for (int i = 0; i < files.length; i++) {
            File songDir = files[i];
            if (!songDir.isDirectory()) {
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
        SongInfo main = Utils.getMainInfoFile(songDir);
        if (main.getMP3() != null) {
            return;
        }

        File[] files = directory.listFiles();

        outer:
        for (File songFile : files) {
            if (songFile.isDirectory()) {
                continue;
            }

            String fileName = songFile.getName().toLowerCase();
            fileName = Normalizer.normalize(fileName, Normalizer.Form.NFD);

            if ( !fileName.endsWith(".mp3") ) {
                continue;
            }

            // check title
            String title = Normalizer.normalize(main.getTitle().toLowerCase(), Normalizer.Form.NFD);
            if (!fileName.contains(title)) {
                continue;
            }

            // check artists
            String[] artists = main.getArtist().toLowerCase().split(Pattern.quote(" "));
            for (String artist : artists) {
                artist = artist.replace(",", "");
                artist = artist.replace("&", "");
                artist = artist.replace(".", "");
                artist = artist.trim();

                if ( artist.equals("feat") || artist.equals("ft") ) {
                    continue;
                }

                if (!fileName.contains(artist)) {
                    continue outer;
                }
            }

            FileUtils.moveFile(songFile, new File(songDir, main.getFileName() + ".mp3"));
            reformat.process(songDir);
            return;
        }

        System.out.println("No mp3 file found for " + songDir.getName() + ".");
    }

}
