package be.jorisg.ultrastarorganizer.commands.reformat;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import be.jorisg.ultrastarorganizer.domain.Library;
import be.jorisg.ultrastarorganizer.domain.TrackDirectory;
import be.jorisg.ultrastarorganizer.domain.TrackInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import picocli.CommandLine;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.info.VideoInfo;
import ws.schild.jave.info.VideoSize;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@CommandLine.Command(name = "reformat",
        description = "Reformat song directory and update info files")
public class ReformatCommand implements Runnable {

    @Override
    public void run() {
        Library library = UltrastarOrganizer.refresh();
        for (TrackDirectory td : library.trackDirectories()) {
            try {
                process(td);
            } catch (Exception e) {
                UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                        "@|red ERROR: " + e.getMessage() + "|@"));
            }
        }

        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                "@|cyan Library has been reformatted. |@"));
    }

    public void process(TrackDirectory td) throws Exception {
        for (TrackInfo ti : td.tracks()) {
            try {
                process(ti);
            } catch (Exception e) {
                e.printStackTrace();
                UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|red ERROR: " + e.getMessage() + "|@"));
            }
        }

        TrackInfo main = td.originalTrack();
        if (!td.directory().getName().equals(main.safeName())) {
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|yellow Moving '" + td.directory().getName() + "' to '" + main.safeName() + "'.|@"));
            td.moveTo(new File(td.directory().getParentFile(), main.safeName()));
        }
    }

    public void process(TrackInfo ti) throws IOException {
        // update title
        String title = ti.title();
        title = title.replaceAll("(?i)[(\\[{]duet[)\\]}]", "").trim();
        title = title.replaceAll(Pattern.quote("  "), " ").trim();
        ti.setTitle(title);

        // process files
        audio(ti);
        video(ti);
        coverImage(ti);
        backgroundImage(ti);

        // update TrackInfo txt file name
        String name = ti.safeName();
        if (ti.isDuet()) {
            name += " (Duet)";
        }
        File target = new File(ti.parentDirectory(), name + ".txt");
        if (!ti.file().equals(target)) {
            int i = 1;
            while (target.exists()) {
                target = new File(ti.parentDirectory(), name + " (" + i + ").txt");
                i++;
            }

            ti.moveTo(target);
        }

        // save file
        ti.save();
    }

    private void audio(TrackInfo ti) throws IOException {
        rename(ti.audioFile(), ti.safeName(), ti::setAudioFileName);
    }

    private static final Map<VideoSize, String> VIDEO_SIZE_NAMES = new HashMap<>();

    static {
        Class<VideoSize> cls = VideoSize.class;
        Arrays.stream(cls.getDeclaredFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()))
                .filter(f -> f.getType() == cls)
                .forEach(f -> {
                    String name = f.getName().toUpperCase();
                    name = name.replaceFirst("TWO", "2");
                    name = name.replaceFirst("FOUR", "4");
                    name = name.replaceFirst("SIXTEEN", "16");
                    try {
                        VIDEO_SIZE_NAMES.put((VideoSize) f.get(cls), name);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private void video(TrackInfo ti) throws IOException {
        String name = ti.safeName();
        if (ti.videoFile() != null) {
            try {
                VideoInfo video = new MultimediaObject(ti.videoFile()).getInfo().getVideo();
                String quality = VIDEO_SIZE_NAMES.get(video.getSize());
                if (quality == null) {
                    quality = video.getSize().getHeight() + "p";
                }
                name = name + String.format(" [%s %.0ffps]", quality, video.getFrameRate());
            } catch (Exception ignored) {
            }
        }
        rename(ti.videoFile(), name, ti::setVideoFileName);
    }

    private void coverImage(TrackInfo ti) throws IOException {
        rename(ti.coverImageFile(), ti.safeName() + " [CO]", ti::setCoverImageFileName);
    }

    private void backgroundImage(TrackInfo ti) throws IOException {
        rename(ti.backgroundImageFile(), ti.safeName() + " [BG]", ti::setBackgroundImageFileName);
    }

    private void rename(File file, String name, Consumer<String> updater) throws IOException {
        if (file == null) {
            return;
        }

        String ext = FilenameUtils.getExtension(file.getName());
        name = name + "." + ext;

        if (file.getName().equals(name)) {
            return;
        }

        File target = new File(file.getParentFile(), name);
        if (target.exists()) {
            return;
        }

        FileUtils.moveFile(file, target);
        updater.accept(name);
    }

}
