package be.jorisg.ultrastarorganizer.commands.tracklist;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.FileSystems;

@CommandLine.Command(name = "tracklist",
        description = "Create a document with a list of all songs.")
public class TracklistCommand implements Runnable {

    @CommandLine.Option(names = {"-t", "--type"}, description = "Output file type",
            required = true, type = TracklistType.class)
    private TracklistType type;

    @CommandLine.Option(names = {"--output"}, description = "Output directory/file")
    private File outputFile;

    @Override
    public void run() {
        if ( outputFile == null ) {
            outputFile = new File(UltrastarOrganizer.workDir, "tracklist." + type.name().toLowerCase());
        } else if ( outputFile.isDirectory() ) {
            outputFile = new File(outputFile, "tracklist." + type.name().toLowerCase());
        }
        
        UltrastarOrganizer.refresh();

        try {
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }

            type.generator().generate(outputFile, UltrastarOrganizer.library().tracks());

            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                    "@|cyan Generated tracklist '" + outputFile.toPath() + "'. |@"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
