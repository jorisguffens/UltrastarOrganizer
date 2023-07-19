package be.jorisg.ultrastarorganizer.commands.minimize;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import be.jorisg.ultrastarorganizer.domain.TrackInfo;
import be.jorisg.ultrastarorganizer.utils.Utils;
import org.apache.commons.io.FileUtils;
import picocli.CommandLine;
import ws.schild.jave.MultimediaObject;

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

    @CommandLine.Option(names = {"--convert-video"}, description = "Convert video codec to smaller file size.")
    private boolean convertVideo = false;

    @Override
    public void run() {
//        try {
//            Encoder en = new Encoder();
//            System.out.println(String.join(", ", en.getSupportedEncodingFormats()));
//        } catch (EncoderException e) {
//            throw new RuntimeException(e);
//        }

        UltrastarOrganizer.refresh();
        long size = FileUtils.sizeOfDirectory(UltrastarOrganizer.library().directory());
        UltrastarOrganizer.library().tracks().forEach(this::process);

        long newSize = FileUtils.sizeOfDirectory(UltrastarOrganizer.library().directory());
        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                String.format("@|cyan Reduced library size from %.2f GB to %.2f GB (%.2f %%).|@",
                        size / 1024.d / 1024.d / 1024.d,
                        newSize / 1024.d / 1024.d / 1024.d,
                        (1 - (size / (double) newSize)) * 100)));
    }

    private void process(TrackInfo ti) {

        File background = ti.backgroundImageFile();
        if (background != null) {
            if (removeBackgrounds) {
                background.delete();
            } else if (backgroundSize > 0) {
                shrinkImage(background, background, backgroundSize);
            }
        }

        File cover = ti.coverImageFile();
        if (cover != null && cover.exists() && coverSize > 0) {
            shrinkImage(cover, cover, coverSize);
        }

        File video = ti.videoFile();
        if (video != null && convertVideo) {
            video(ti);
        }

        File audio = ti.audioFile();
        if (audio != null) {
            audio(ti);
        }

    }

    public static void video(TrackInfo ti) {
        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                "@|yellow Compressing video of|@ @|magenta " + ti.name() + "|@"));

        File src = ti.videoFile();
        Utils.shrinkVideo(ti);
        File dest = ti.videoFile();

        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(String.format(
                "@|yellow   -> Reduced filesize of %s from %.2f MB to %.2f MB.|@",
                ti.name(),
                FileUtils.sizeOf(src) / 1024.d / 1024.d,
                FileUtils.sizeOf(dest) / 1024.d / 1024.d)));
    }

    private void audio(TrackInfo ti) {

    }


}
