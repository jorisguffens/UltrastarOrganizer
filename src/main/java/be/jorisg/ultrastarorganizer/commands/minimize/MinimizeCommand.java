package be.jorisg.ultrastarorganizer.commands.minimize;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import be.jorisg.ultrastarorganizer.domain.TrackDirectory;
import be.jorisg.ultrastarorganizer.domain.TrackInfo;
import be.jorisg.ultrastarorganizer.search.SearchEngine;
import picocli.CommandLine;
import ucar.nc2.util.IO;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static be.jorisg.ultrastarorganizer.utils.Utils.shrinkImage;

@CommandLine.Command(name = "minimize",
        description = "Minimize library by removing stuff and compressing images.")
public class MinimizeCommand implements Runnable {

    @CommandLine.Option(names = {"--remove-backgrounds"}, description = "Remove background images")
    private boolean removeBackgrounds;

    @CommandLine.Option(names = {"--image-size"}, description = "Target image size")
    private int imageSize = 256;

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
            } else {
                shrinkImage(background, background, imageSize);
            }
        }

        File cover = ti.coverImageFile();
        if ( cover != null ) {
            shrinkImage(cover, cover, imageSize);
        }

    }


}
