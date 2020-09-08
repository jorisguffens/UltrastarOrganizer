package be.jorisg.ultrastarorganizer;

import be.jorisg.ultrastarorganizer.organizer.LibraryOrganizer;
import be.jorisg.ultrastarorganizer.utils.TableList;
import org.apache.commons.cli.*;

import java.io.File;

public class Main {

    public static void main(String[] args) {

        Options options = new Options();

        Option directoryInput = new Option("d", "directory", true, "songs directory");
        directoryInput.setRequired(true);
        options.addOption(directoryInput);

        Option convertVideoToAudioInput = new Option("cva", "convert-video-to-audio", false, "convert video to mp3 if no mp3s are found");
        convertVideoToAudioInput.setRequired(false);
        options.addOption(convertVideoToAudioInput);

        Option backgroundToCoverInput = new Option("b2c", "background-to-cover", false, "change background files to cover files");
        backgroundToCoverInput.setRequired(false);
        options.addOption(backgroundToCoverInput);

        Option removeVideoInput = new Option("rv", "remove-video", false, "remove video files");
        removeVideoInput.setRequired(false);
        options.addOption(removeVideoInput);

        Option cleanCachesInput = new Option("cc", "clean-caches", false, "clean cache files");
        cleanCachesInput.setRequired(false);
        options.addOption(cleanCachesInput);

        Option createCSVInput = new Option("csv", "create-csv", false, "create csv file");
        createCSVInput.setRequired(false);
        options.addOption(createCSVInput);

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
        boolean convertAudio = cmd.hasOption("convert-video-to-audio");
        boolean background2cover = cmd.hasOption("background-to-cover");
        boolean removeVideo = cmd.hasOption("remove-video");
        boolean cleanCaches = cmd.hasOption("clean-caches");
        boolean createCSV = cmd.hasOption("create-csv");

        File dir = new File(directory);
        if ( !dir.exists() ) {
            System.out.println("Path doesn't exist.");
            return;
        }

        System.out.println("Organizing library...");
        LibraryOrganizer organizer = new LibraryOrganizer(dir);
        organizer.run(createCSV, convertAudio, background2cover, removeVideo, cleanCaches);
        System.out.println("\n\n");

        TableList tl = new TableList(2, "CODE", "Description").sortBy(0);
        tl.addRow("0", "Can't find any info (.txt) files.");
        tl.addRow("1", "Invalid info (.txt) file. It is corrupt or headers are missing.");
        tl.addRow("2", "Too many audio files. It can't decide the correct one.");
        tl.addRow("3", "No audio (.mp3) file. There is no audio file or the video can't be converted.");
        tl.print();
    }

}
