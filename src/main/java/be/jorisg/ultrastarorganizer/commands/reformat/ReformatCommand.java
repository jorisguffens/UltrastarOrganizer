package be.jorisg.ultrastarorganizer.commands.reformat;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import be.jorisg.ultrastarorganizer.domain.*;
import be.jorisg.ultrastarorganizer.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
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
                UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|red ERROR: " + e.getMessage() + "|@"));
            }
        }

        TrackInfo main = td.originalTrack();
        if (!td.directory().getName().equals(main.safeName())) {
            td.moveTo(new File(td.directory().getParentFile(), main.safeName()));
        }
    }

    public void process(TrackInfo ti) throws IOException {
        // update title;
        String title = ti.title();
        title = title.replaceAll("(?i)[(\\[{]duet[)\\]}]", "").trim();
        title = title.replaceAll(Pattern.quote("  "), " ").trim();
        ti.setTitle(title);

        // process files
        audio(ti);
        video(ti);
        coverImage(ti);
        backgroundImage(ti);

        // fix negative lyrics start
        NoteLyricCollection nlc = null;
        try {
            nlc = ti.noteLyrics();
            int beat = nlc.noteLyricBlocks().get(0).noteLyrics().get(0).beat();
            if (beat < 0) {
                ti.setNoteLyrics(nlc.shift(Math.abs(beat)));
                UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                        "@|cyan Shifting beats to a positive number for " + ti.safeName() + "|@"));
            }
            if ( nlc.noteLyricBlocks().stream().anyMatch(nlb -> !nlb.isValid()) ) {
                UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                        "@|yellow WARNING: A note block is invalid in " + ti.safeName() + ". |@"));
            }
        } catch (Exception e) {
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|red ERROR: " + ti.safeName() + ": " +
                    e.getClass().getSimpleName() + ": " + e.getMessage() + "|@"));
            e.printStackTrace(UltrastarOrganizer.out);
        }

        // update duet info
        if ( ti.isDuet() && nlc != null) {
            long blocks = nlc.noteLyricBlocks().stream().filter(b -> b.singer() != null).count();
            if (blocks < 2) {
                UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|yellow WARNING: " + ti.safeName() + ": duet without singer indication. |@"));
            }
            else if ( nlc.noteLyricBlocks().stream().anyMatch(b -> b.format() != NoteLyricBlock.DuetFormat.ULTRASTAR) ) {
                ti.rewriteLyrics();
                UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|yellow WARNING: " + ti.safeName() + ": invalid duet format, lyrics will be rewritten. |@"));
            }
        }

        // update TrackInfo txt file name
        if (!ti.file().getName().equals(ti.safeName() + ".txt")) {
            String name = ti.safeName();
            if ( ti.isDuet() ) name += " (Duet)";
            File target = new File(ti.parentDirectory(), name + ".txt");

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
        if (ti.audioFile() == null) {
            missing(ti, Utils::verifyAudio, "mp3")
                    .ifPresentOrElse(
                            f -> ti.setAudioFileName(f.getName()),
                            () -> UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|yellow WARNING: " + ti.name() + ": No audio file found.|@"))
                    );
        }

        rename(ti.audioFile(), ti.safeName(), ti::setAudioFileName);
    }

    private void video(TrackInfo ti) throws IOException {
        if (ti.videoFile() == null) {
            missing(ti, f -> true, Utils.VIDEO_EXT)
                    .ifPresent(file -> ti.setVideoFileName(file.getName()));
        }

        rename(ti.videoFile(), ti.safeName(), ti::setVideoFileName);
    }

    private void coverImage(TrackInfo ti) throws IOException {
        if (ti.coverImageFile() == null) {
            missing(ti, f -> !f.equals(ti.backgroundImageFile()) && Utils.verifyImage(f), Utils.IMAGE_EXT)
                    .ifPresentOrElse(
                            f -> ti.setCoverImageFileName(f.getName()),
                            () -> UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string("@|yellow WARNING: " + ti.name() + ": No cover image found.|@")));
        }

        rename(ti.coverImageFile(), ti.safeName() + " [CO]", ti::setCoverImageFileName);
    }

    private void backgroundImage(TrackInfo ti) throws IOException {
        if (ti.backgroundImageFile() == null) {
            missing(ti, f -> !f.equals(ti.coverImageFile()) && Utils.verifyImage(f), Utils.IMAGE_EXT)
                    .ifPresent(f -> ti.setBackgroundImageFileName(f.getName()));
        }

        rename(ti.backgroundImageFile(), ti.safeName() + " [BG]", ti::setBackgroundImageFileName);
    }

    private Optional<File> missing(TrackInfo ti, Predicate<File> filter, String... extensions) {
        return Utils.findFilesByExtensions(ti.parentDirectory(), extensions).stream()
                .filter(filter)
                .findFirst();
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
        if ( target.exists() ) {
            return;
        }

        FileUtils.moveFile(file, target);
        updater.accept(name);
    }

}
