package be.jorisg.ultrastarorganizer;

import be.jorisg.ultrastarorganizer.organizer.LibraryOrganizer;
import be.jorisg.ultrastarorganizer.synchronizer.LibrarySynchronizer;
import be.jorisg.ultrastarorganizer.utils.TableList;
import org.apache.commons.cli.*;

import java.io.File;

public class Main {

    public static void main(String[] args) {

        Options options = new Options();

        Option directoryInput = new Option("d", "directory", true, "songs directory");
        directoryInput.setRequired(true);
        options.addOption(directoryInput);

        Option convertAudioInput = new Option("ca", "convertaudio", false, "convert video to mp3 if no mp3s are found");
        convertAudioInput.setRequired(false);
        options.addOption(convertAudioInput);

        Option removeVideoInput = new Option("rv", "removevideo", false, "remove video files");
        removeVideoInput.setRequired(false);
        options.addOption(removeVideoInput);

        Option cleanCachesInput = new Option("cc", "cleancaches", false, "clean cache files");
        cleanCachesInput.setRequired(false);
        options.addOption(cleanCachesInput);

        Option syncInput = new Option("s", "synchronize", true, "off,search,update");
        syncInput.setRequired(false);
        options.addOption(syncInput);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
            return;
        }

        String directory = cmd.getOptionValue("directory");
        boolean convertAudio = cmd.hasOption("convertaudio");
        boolean removeVideo = cmd.hasOption("removevideo");
        boolean cleanCaches = cmd.hasOption("cleancaches");

        File dir = new File(directory);
        if ( !dir.exists() ) {
            System.out.println("Path doesn't exist.");
            return;
        }

        LibraryOrganizer organizer = new LibraryOrganizer(dir);
        organizer.run(convertAudio, removeVideo, cleanCaches);

        String sync = cmd.getOptionValue("synchronize");
        if ( sync != null && (sync.equals("search") || sync.equals("update")) ) {
            LibrarySynchronizer synchronizer = new LibrarySynchronizer(dir);
            synchronizer.run(sync.equals("update"));
        }

        TableList tl = new TableList(2, "CODE", "Description").sortBy(0);
        tl.addRow("0", "Can't find any info (.txt) files.");
        tl.addRow("1", "Invalid info (.txt) file. It is corrupt or headers are missing.");
        tl.addRow("2", "Too many audio files. It can't decide the correct one.");
        tl.addRow("3", "No audio (.mp3) file. There is no audio file or the video can't be converted.");

        System.out.println("\n\n");
        tl.print();
    }

}
