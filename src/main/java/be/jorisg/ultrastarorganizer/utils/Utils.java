package be.jorisg.ultrastarorganizer.utils;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import be.jorisg.ultrastarorganizer.domain.TrackInfo;
import org.apache.commons.io.FilenameUtils;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.process.ProcessLocator;
import ws.schild.jave.process.ProcessWrapper;
import ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {

    public final static String[] AUDIO_EXT = new String[]{"mp3"};
    public final static String[] VIDEO_EXT = new String[]{"mp4", "avi", "mkv", "flv", "mov", "mpg", "m4v", "divx", "webm"};
    public final static String[] IMAGE_EXT = new String[]{"jpg", "png", "jpeg", "jfif"};

    private final static ProcessLocator locator = new DefaultFFMPEGLocator();

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

    private static boolean verify(File file, String... mediaTypes) {
        try {
            String mediaType = Files.probeContentType(file.toPath());
            return mediaType != null && Arrays.stream(mediaTypes).anyMatch(mediaType::contains);
        } catch (IOException e) {
            e.printStackTrace(UltrastarOrganizer.out);
        }
        return false;
    }

    public static boolean verifyVideo(File file) {
        return verify(file, "video/", "application/octet-stream");
    }

    public static boolean verifyAudio(File file) {
        return verify(file, "media/mp3", "audio/mpeg");
    }

    public static boolean verifyImage(File file) {
        return verify(file, "image/png", "image/jpeg");
    }

    public static boolean shrinkImage(File file, File outputFile, int maxSize) {
        try {
            BufferedImage original = ImageIO.read(file);
            if (original == null) {
                return false;
            }

            double ratio = (double) original.getHeight() / (double) original.getWidth();
            int targetWidth, targetHeight;
            if (ratio > 1) {
                targetWidth = (int) (maxSize / ratio);
                targetHeight = maxSize;
            } else {
                targetWidth = maxSize;
                targetHeight = (int) (maxSize * ratio);
            }

            if (original.getWidth() < targetWidth || original.getHeight() < targetHeight) {
                return false;
            }

            Image resultingImage = original.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
            BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);

            ImageIO.write(outputImage, "jpeg", outputFile);
            return true;
        } catch (IOException ignored) {
        }
        return false;
    }

    public static boolean shrinkVideo(TrackInfo ti) throws Exception {
        File src = ti.videoFile();

        MultimediaObject mo = new MultimediaObject(src);
        if (mo.getInfo().getVideo().getDecoder().equals("webm")) {
            return false;
        }

        try (ProcessWrapper ffmpeg = locator.createExecutor();) {
            File dest = new File(ti.parentDirectory(), ti.safeName() + ".min.webm");
            if (dest.exists()) {
                return false;
            }

            List.of(
                    "-i", src.getAbsolutePath(),
                    "-c:v", "libvpx",
                    "-b:v", "192KB", // ~11MB filesize per minute
                    "-crf", "20",
                    "-f", "webm",
                    "-y",
                    dest.getAbsolutePath()
            ).forEach(ffmpeg::addArgument);
            ffmpeg.execute();

            PrintWriter writer = new PrintWriter(UltrastarOrganizer.out) {
                @Override
                public void write(String s) {
                    if (s.contains("frame")) {
                        super.write(s);
                    }
                }

            };

            InputStreamReader reader = new InputStreamReader(ffmpeg.getErrorStream());
            reader.transferTo(writer);

            int exitCode = ffmpeg.getProcessExitCode();
            if (exitCode != 0) {
                throw new RuntimeException("Exit code of ffmpeg encoding run is " + exitCode);
            }

            ti.setVideoFileName(dest.getName());
            ti.save();
        }

        return true;
    }

    public static void extractAudioFromVideo(File video, File audio) throws Exception {
        try (ProcessWrapper ffmpeg = locator.createExecutor()) {
            List.of(
                    "-i", video.getAbsolutePath(), audio.getAbsolutePath()
            ).forEach(ffmpeg::addArgument);
            ffmpeg.execute();

            int exitCode = ffmpeg.getProcessExitCode();
            if (exitCode != 0) {
                throw new RuntimeException("Exit code of ffmpeg encoding run is " + exitCode);
            }
        }
    }

    public static void copyFolder(Path source, Path target, CopyOption... options)
            throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.copy(file, target.resolve(source.relativize(file).toString()), options);
                return FileVisitResult.CONTINUE;
            }
        });
    }

}
