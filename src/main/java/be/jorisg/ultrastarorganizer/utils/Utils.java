package be.jorisg.ultrastarorganizer.utils;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import be.jorisg.ultrastarorganizer.domain.TrackInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import picocli.CommandLine;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Utils {

    public final static String[] VIDEO_EXT = new String[]{"mp4", "avi", "mkv", "flv", "mov", "mpg", "m4v", "divx"};
    public final static String[] IMAGE_EXT = new String[]{"jpg", "png", "jpeg", "jfif"};

    private final static Tika tika = new Tika();

    public static List<File> findFilesByExtensions(File directory, String... extensions) {
        File[] files = directory.listFiles();
        if (files == null) {
            return new ArrayList<>();
        }

        return Arrays.stream(files).filter(file -> {
            String ext = FilenameUtils.getExtension(file.getName());
            return Arrays.stream(extensions).anyMatch(e -> e.equalsIgnoreCase(ext));
        }).collect(Collectors.toList());
    }

    public static boolean verifyAudio(File file) {
        try {
            String mediaType = tika.detect(file);
            return mediaType.equals("media/mp3") || mediaType.equals("audio/mpeg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean verifyImage(File file) {
        try {
            String mediaType = tika.detect(file);
            return mediaType.equals("image/png") || mediaType.equals("image/jpeg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean shrinkImage(File file, File outputFile, int maxSize) {
        try {
            BufferedImage original = ImageIO.read(file);
            if (original == null) {
                return false;
            }

            double ratio = (double) original.getHeight() / (double) original.getWidth();
            int targetWidth, targetHeight;
            if ( ratio > 1 ) {
                targetWidth = (int) (maxSize / ratio);
                targetHeight = maxSize;
            } else {
                targetWidth = maxSize;
                targetHeight = (int) (maxSize * ratio);
            }

            if ( original.getWidth() < targetWidth || original.getHeight() < targetHeight ) {
                return false;
            }

            Image resultingImage = original.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
            BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);

            ImageIO.write(outputImage, "jpeg", outputFile);
            return true;
        } catch (IOException ignored) {}
        return false;
    }

    public static void shrinkVideo(TrackInfo ti) {
        File video = ti.videoFile();
        MultimediaObject mo;
        try {
            mo = new MultimediaObject(video);
        } catch (Exception ex) {
            return;
        }

        try {
            File dest = new File(ti.parentDirectory(), ti.safeName() + " [tmp].mp4");
            Encoder encoder = new Encoder();
            EncodingAttributes attrs = new EncodingAttributes()
                    .setOutputFormat("mp4")
                    .setVideoAttributes(new VideoAttributes().setCodec("h264"))
                    .setAudioAttributes(new AudioAttributes());
            encoder.encode(mo, dest, attrs);

            video.delete();

            File target = new File(ti.parentDirectory(), ti.safeName() + ".mp4");
            dest.renameTo(target);

            ti.setVideoFileName(target.getName());
            ti.save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
