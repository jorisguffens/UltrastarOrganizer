package be.jorisg.ultrastarorganizer.commands.doctor;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import be.jorisg.ultrastarorganizer.domain.Library;
import be.jorisg.ultrastarorganizer.domain.NoteLyricBlock;
import be.jorisg.ultrastarorganizer.domain.NoteLyricCollection;
import be.jorisg.ultrastarorganizer.domain.TrackInfo;
import be.jorisg.ultrastarorganizer.utils.Utils;
import com.drew.lang.annotations.NotNull;
import org.apache.commons.io.FileUtils;
import picocli.CommandLine;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@CommandLine.Command(name = "doctor",
        description = "Check your library for problems and try to fix them.")
public class DoctorCommand implements Runnable {

    @CommandLine.Option(names = {"--dry-run"}, description = "Print problems but don't fix them.")
    private boolean dryRun = false;

    @Override
    public void run() {
        Library library = UltrastarOrganizer.refresh();

        int tracks = 0;
        int issues = 0;

        for (TrackInfo ti : library.tracks()) {
            int i = process(ti);
            if (i > 0) {
                tracks++;
                issues += i;
            }
        }

        // remove empty directories or directories with corrupt tracks
        for (File f : library.directory().listFiles()) {
            if (!f.isDirectory()) {
                continue;
            }

            try {
                if (f.listFiles().length == 0) {
                    if (!dryRun) {
                        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                                "@|yellow " + f.getName() + " is an empty directory -> REMOVED |@"));
                        FileUtils.deleteDirectory(f);
                    } else {
                        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                                "@|yellow " + f.getName() + " is an empty directory. |@"));
                    }
                    continue;
                }

                if (library.trackDirectories().stream().noneMatch(td -> td.directory().equals(f))) {
                    if (!dryRun) {
                        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                                "@|yellow The directory " + f.getName() + " has no tracks -> REMOVED |@"));
                        FileUtils.deleteDirectory(f);
                    } else {
                        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                                "@|yellow The directory " + f.getName() + " has no tracks. |@"));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                String.format("@|cyan Found %d issues in %d tracks.|@", issues, tracks)));

        long size = FileUtils.sizeOfDirectory(library.directory());
        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                String.format("@|cyan Your library is %.2f GB in size.|@", size / 1024.d / 1024.d / 1024.d)));
    }

    private int process(TrackInfo ti) {
        List<String> issues = new ArrayList<>();

        audio(ti, issues);
        video(ti, issues);
        cover(ti, issues);
        background(ti, issues);
        lyrics(ti, issues);

        if (issues.isEmpty()) {
            return 0;
        }

        UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                String.format("@|red Issues for |@ @|magenta %s|@@|red :|@", ti.name())));
        for (String msg : issues) {
            UltrastarOrganizer.out.println(CommandLine.Help.Ansi.AUTO.string(
                    String.format("@|yellow - %s|@", msg)));
        }

        ti.save();

        return issues.size();
    }

    private void audio(@NotNull TrackInfo ti, @NotNull List<String> issues) {
        file(ti.audioFile(),
                () -> missing(ti, Utils::verifyAudio, "mp3").orElse(null),
                ti::setAudioFileName,
                f -> {
                    ;
                }, //new MultimediaObject(f).getInfo().getAudio(),
                "Audio file",
                issues);
    }

    private void video(@NotNull TrackInfo ti, @NotNull List<String> issues) {
        file(ti.videoFile(),
                () -> missing(ti, Utils::verifyVideo, Utils.VIDEO_EXT).orElse(null),
                ti::setVideoFileName,
                f -> {
                    ;
                }, //new MultimediaObject(f).getInfo().getVideo(),
                "Video file",
                issues);
    }

    private void cover(@NotNull TrackInfo ti, @NotNull List<String> issues) {
        file(ti.coverImageFile(),
                () -> missing(ti, f -> !f.equals(ti.backgroundImageFile()) && Utils.verifyImage(f), Utils.IMAGE_EXT)
                        .orElse(null),
                ti::setCoverImageFileName,
                f -> ImageIO.read(ti.coverImageFile()),
                "Cover image",
                issues);
    }

    private void background(@NotNull TrackInfo ti, @NotNull List<String> issues) {
        file(ti.backgroundImageFile(),
                () -> missing(ti, f -> !f.equals(ti.coverImageFile()) && Utils.verifyImage(f), Utils.IMAGE_EXT)
                        .orElse(null),
                ti::setBackgroundImageFileName,
                f -> ImageIO.read(ti.backgroundImageFile()),
                "Background image",
                issues);
        issues.remove("Background image is missing.");
    }

    private void file(@NotNull File file,
                      @NotNull Supplier<File> finder,
                      @NotNull Consumer<String> setter,
                      @NotNull ThrowingConsumer<File> tester,
                      @NotNull String name,
                      @NotNull List<String> issues) {

        if (file != null && file.exists()) {
            try {
                tester.accept(file);
                return;
            } catch (Exception ignored) {
            }

            if (!dryRun) {
                issues.add(name + " is invalid -> REMOVED");
                file.delete();
                setter.accept(null);
            } else {
                issues.add(name + " is invalid.");
            }
        }

        file = finder.get();
        if (file == null) {
            issues.add(name + " is missing.");
            return;
        }

        try {
            tester.accept(file);
        } catch (Exception ex) {
            issues.add(name + " is missing.");
            return;
        }

        if (!dryRun) {
            issues.add(name + " is missing -> FIXED");
            setter.accept(file.getName());
        } else {
            issues.add(name + " is missing -> MATCH FOUND");
        }

    }

    private void lyrics(@NotNull TrackInfo ti, @NotNull List<String> issues) {
        // LYRICS
        NoteLyricCollection nlc = null;
        try {
            nlc = ti.noteLyrics();
            int beat = nlc.noteLyricBlocks().get(0).noteLyrics().get(0).beat();
            if (beat < 0) {
                if (!dryRun) {
                    issues.add("First beat was negative -> FIXED");
                    ti.setNoteLyrics(nlc.shift(Math.abs(beat)));
                } else {
                    issues.add("First beat is negative.");
                }
            }
            if (nlc.noteLyricBlocks().stream().anyMatch(nlb -> !nlb.isValid())) {
                issues.add("Lyrics contain invalid note blocks.");
            }

            long blocks = nlc.noteLyricBlocks().stream().filter(b -> b.singer() != null).count();
            if (ti.isDuet()) {
                if (blocks < 2) {
                    issues.add("Duet with less than 2 blocks.");
                } else if (nlc.noteLyricBlocks().stream().anyMatch(b -> b.format() != NoteLyricBlock.DuetFormat.ULTRASTAR)) {
                    if (!dryRun) {
                        issues.add("Duet with invalid format -> FIXED");
                        ti.convertDuetFormat();
                    } else {
                        issues.add("Duet with invalid format.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace(UltrastarOrganizer.out);
        }
    }

    private Optional<File> missing(TrackInfo ti, Predicate<File> filter, String... extensions) {
        return Utils.findFilesByExtensions(ti.parentDirectory(), extensions).stream()
                .filter(filter)
                .findFirst();
    }


    private interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

}
