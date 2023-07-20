package be.jorisg.ultrastarorganizer.commands.minimize;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import be.jorisg.ultrastarorganizer.domain.TrackDirectory;
import be.jorisg.ultrastarorganizer.domain.TrackInfo;
import be.jorisg.ultrastarorganizer.utils.Utils;
import org.apache.commons.io.FileUtils;
import picocli.CommandLine;
import ws.schild.jave.MultimediaObject;

import java.io.File;
import java.util.Arrays;

import static be.jorisg.ultrastarorganizer.utils.Utils.shrinkImage;

@CommandLine.Command(name = "minimize",
        description = "Minimize library by removing stuff and compressing images.")
public class MinimizeCommand implements Runnable {

    @CommandLine.Option(names = {"--remove"}, description = "Remove files, options are: unused, background, cover, video, audio")
    private String[] remove;

    @CommandLine.Option(names = {"--optimize"}, description = "Which files to optimize (resize or re-encode), options are: cover, background, video.")
    private String[] optimize;

    @Override
    public void run() {
        UltrastarOrganizer.refresh();
        long size = FileUtils.sizeOfDirectory(UltrastarOrganizer.library().directory());
        UltrastarOrganizer.library().trackDirectories().forEach(this::process);

        long newSize = FileUtils.sizeOfDirectory(UltrastarOrganizer.library().directory());
        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                String.format("@|cyan Reduced library size from %.2f GB to %.2f GB (%.2f %%).|@",
                        size / 1024.d / 1024.d / 1024.d,
                        newSize / 1024.d / 1024.d / 1024.d,
                        (1 - (newSize / (double) size)) * 100)));
    }

    private void process(TrackDirectory td) {
        for ( TrackInfo ti : td.tracks() ) {
            process(ti);
        }

        // unused files
        for ( File f : td.directory().listFiles() ) {
            if ( f.isDirectory() ) {
                continue;
            }
            if ( td.tracks().stream().anyMatch(ti -> f.equals(ti.file()) || f.equals(ti.videoFile()) || f.equals(ti.audioFile())
                    || f.equals(ti.backgroundImageFile()) || f.equals(ti.coverImageFile())) ) {
                continue;
            }

            if ( shouldRemove("unused") ) {
                UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                        "@|yellow The file " + f.getName() + " is never referenced -> REMOVED |@"));
                f.delete();
            }
        }
    }

    private void process(TrackInfo ti) {
        File background = ti.backgroundImageFile();
        if (background != null) {
            if ( shouldRemove("background") ) {
                UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                        "@|yellow Background file " + background.getName() + " -> REMOVED |@"));
                background.delete();
            }
            else if ( shouldOptimize("background") ) {
                shrinkImage(background, background, 1920); // 1920x1080
            }
        }

        File cover = ti.coverImageFile();
        if (cover != null && cover.exists() ) {
            if ( shouldRemove("cover") ) {
                UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                        "@|yellow Cover file " + cover.getName() + " -> REMOVED |@"));
                cover.delete();
            }
            else if ( shouldOptimize("cover") ) {
                shrinkImage(cover, cover, 256); // 256 x 256
            }
        }

        File video = ti.videoFile();
        if (video != null ) {
            if ( shouldRemove("video") ) {
                UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                        "@|yellow Video file " + video.getName() + " -> REMOVED |@"));
                video.delete();
            }
            else if ( shouldOptimize("video") ) {
                video(ti);
            }
        }

        // TODO audio
    }

    public static void video(TrackInfo ti) {
        try {
            long size = ti.videoFile().length();
            long time = System.currentTimeMillis();

            if ( Utils.shrinkVideo(ti) ) {
                long duration = System.currentTimeMillis() - time;
                long newSize = ti.videoFile().length();
                UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(String.format(
                        "@|yellow Reduced filesize of |@@|magenta %s|@ @|yellow from %.2f MB to %.2f MB in %.2f seconds.|@",
                        ti.name(),
                        size / 1024.d / 1024.d,
                        newSize / 1024.d / 1024.d,
                        duration / 1000.d)));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean shouldRemove(String key) {
        return remove != null && Arrays.stream(remove).anyMatch(s -> s.equalsIgnoreCase(key));
    }

    private boolean shouldOptimize(String key) {
        return optimize != null && Arrays.stream(optimize).anyMatch(s -> s.equalsIgnoreCase(key));
    }

}
