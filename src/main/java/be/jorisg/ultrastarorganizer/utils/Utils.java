package be.jorisg.ultrastarorganizer.utils;

import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;

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

    public static Runnable uncheck(ThrowingRunnable r) {
        return () -> {
            try {
                r.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static <T> Consumer<T> uncheck(ThrowingConsumer<T> c) {
        return (v) -> {
            try {
                c.accept(v);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    public interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

}
