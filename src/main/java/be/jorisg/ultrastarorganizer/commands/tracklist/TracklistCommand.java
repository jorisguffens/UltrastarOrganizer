package be.jorisg.ultrastarorganizer.commands.tracklist;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import picocli.CommandLine;

import java.io.File;
import java.util.Arrays;

@CommandLine.Command(name = "tracklist",
        description = "Create a document with a list of all songs.")
public class TracklistCommand implements Runnable {

    @CommandLine.Option(names = {"--output"}, description = "Output directory/file")
    private File outputFile;

    @Override
    public void run() {
        UltrastarOrganizer.refresh();

        if ( outputFile == null ) {
            UltrastarOrganizer.library().tracks().forEach(track -> UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                    "@|cyan " + track.title() + " |@")));
            return;
        }

        TracklistType type = Arrays.stream(TracklistType.values())
                .filter(t -> outputFile.getName().toLowerCase().endsWith("." + t.name().toLowerCase()))
                .findFirst().orElse(null);
        if (type == null) {
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                    "@|cyan Unsupported output format, valid options are: csv, odt. |@"));
            return;
        }

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
