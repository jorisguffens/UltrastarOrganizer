package be.jorisg.ultrastarorganizer.commands.minimize;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import be.jorisg.ultrastarorganizer.domain.TrackInfo;
import org.apache.commons.io.FileUtils;
import picocli.CommandLine;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;

import java.io.File;
import java.util.Map;

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
        File video = ti.videoFile();
        MultimediaObject mo;
        try {
            mo = new MultimediaObject(video);
        } catch (Exception ex) {
            return;
        }

        try {
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                    "@|yellow Compressing video of " + ti.name() + "... |@"));

            File dest = new File(ti.parentDirectory(), ti.safeName() + " [tmp].mp4");
            Encoder encoder = new Encoder();
            EncodingAttributes attrs = new EncodingAttributes()
                    .setOutputFormat("mp4")
                    .setVideoAttributes(new VideoAttributes().setCodec("h264"))
                    .setAudioAttributes(new AudioAttributes());
            encoder.encode(mo, dest, attrs);

            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(String.format(
                    "@|yellow   -> Reduced filesize of %s from %.2f MB to %.2f MB.|@",
                    ti.name(),
                    FileUtils.sizeOf(ti.videoFile()) / 1024.d / 1024.d,
                    FileUtils.sizeOf(dest) / 1024.d / 1024.d)));

            video.delete();

            File target = new File(ti.parentDirectory(), ti.safeName() + ".mp4");
            dest.renameTo(target);

            ti.setVideoFileName(target.getName());
            ti.save();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // TODO shrink
    }

    private void audio(TrackInfo ti) {
        File audio = ti.audioFile();
        MultimediaObject mo;
        try {
            mo = new MultimediaObject(audio);
            mo.getInfo().getAudio().getDecoder();
        } catch (Exception ex) {
            return;
        }

        // TODO shrink
    }


}
