package be.jorisg.ultrastarorganizer.commands.minimize;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import be.jorisg.ultrastarorganizer.domain.TrackInfo;
import picocli.CommandLine;

import java.io.File;

import static be.jorisg.ultrastarorganizer.utils.Utils.shrinkImage;

@CommandLine.Command(name = "minimize",
        description = "Minimize library by removing stuff and compressing images.")
public class MinimizeCommand implements Runnable {

    @CommandLine.Option(names = {"--remove-backgrounds"}, description = "Remove background images.")
    private boolean removeBackgrounds;

    @CommandLine.Option(names = {"--max-cover-size"}, description = "Maximum cover image size.")
    private int coverSize = 0;

    @CommandLine.Option(names = {"--max-background-size"}, description = "Maximum background image size.")
    private int backgroundSize = 0;

    @Override
    public void run() {
        UltrastarOrganizer.refresh();
        UltrastarOrganizer.library().tracks().forEach(this::process);
    }

    private void process(TrackInfo ti) {

        File background = ti.backgroundImageFile();
        if ( background != null ) {
            if (removeBackgrounds) {
                background.delete();
            } else if ( backgroundSize > 0 ) {
                shrinkImage(background, background, backgroundSize);
            }
        }

        File cover = ti.coverImageFile();
        if ( cover != null && coverSize > 0 ) {
            shrinkImage(cover, cover, coverSize);
        }

    }


}
