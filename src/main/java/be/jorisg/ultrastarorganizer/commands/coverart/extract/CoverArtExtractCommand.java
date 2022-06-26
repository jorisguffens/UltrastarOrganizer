package be.jorisg.ultrastarorganizer.commands.coverart.extract;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import be.jorisg.ultrastarorganizer.domain.TrackInfo;
import be.jorisg.ultrastarorganizer.utils.Utils;
import picocli.CommandLine;

import java.io.File;

@CommandLine.Command(name = "extract",
        description = "Copy and bundle cover images of all songs into a single directory.")
public class CoverArtExtractCommand implements Runnable {

    @CommandLine.Option(names = "--output-dir", description = "The output directory")
    private File outputDir;

    @CommandLine.Option(names = "--image-size", description = "Size of the cover images.")
    private int imageSize = 128;

    @Override
    public void run() {
        if (outputDir == null) {
            outputDir = new File(UltrastarOrganizer.workDir, "#ExtractedCovers");
        }
        if ( !outputDir.exists() ) {
            outputDir.mkdirs();
        } else if (!outputDir.isDirectory()) {
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@| ERROR: Given output path is not a directory. |@"));
            return;
        }

        UltrastarOrganizer.refresh().tracks().forEach(this::process);

        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                "@|cyan Covers have been extracted and resized in '" + outputDir.toPath() + "'. |@"));
    }

    private void process(TrackInfo ti) {
        if (ti.coverImageFile() == null) {
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|yellow WARNING: " + ti.safeName() + ": No cover image file set. |@"));
            return;
        }

        Utils.shrinkImage(ti.coverImageFile(), new File(outputDir, ti.safeName() + ".jpg"), imageSize);
    }

}
