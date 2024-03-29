package be.jorisg.ultrastarorganizer.commands.doctor;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import be.jorisg.ultrastarorganizer.domain.Library;
import be.jorisg.ultrastarorganizer.domain.NoteLyric;
import be.jorisg.ultrastarorganizer.domain.NoteLyricCollection;
import be.jorisg.ultrastarorganizer.domain.TrackInfo;
import be.jorisg.ultrastarorganizer.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@CommandLine.Command(name = "doctor",
        description = "Check your library for problems and try to fix them.")
public class DoctorCommand implements Runnable {

    @CommandLine.Option(names = {"--dry-run"}, description = "Print problems but don't fix them.")
    private boolean dryRun = false;

    @CommandLine.Option(names = {"--ignore", "-i"}, description = "Don't fix or print problems for: cover, background, audio, video, lyrics.")
    private String[] ignore = new String[0];

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
        if (shouldIgnore("audio")) {
            return;
        }

        file(ti.audioFile(),
                () -> missing(ti, Utils::verifyAudio, Utils.AUDIO_EXT).orElse(null),
                ti::setAudioFileName,
                f -> {
                    if (!Utils.verifyAudio(f)) {
                        throw new RuntimeException("File does not contain audio track.");
                    }
                },
                "Audio file",
                issues);

        if (ti.audioFile() != null || ti.videoFile() == null) {
            return;
        }

        issues.removeLast();
        File dest = new File(ti.parentDirectory(), ti.safeName() + ".mp3");
        try {
            Utils.extractAudioFromVideo(ti.videoFile(), dest);
            issues.add("Audio file is missing -> FIXED");
        } catch (Exception e) {
            issues.add("Audio file is missing -> FAILED: " + e.getMessage());
        }
    }

    private void video(@NotNull TrackInfo ti, @NotNull List<String> issues) {
        if (shouldIgnore("video")) {
            return;
        }

        file(ti.videoFile(),
                () -> missing(ti, Utils::verifyVideo, Utils.VIDEO_EXT).orElse(null),
                ti::setVideoFileName,
                f -> {
                    if (!Utils.verifyVideo(f)) {
                        throw new RuntimeException("File does not contain video track.");
                    }
                },
                "Video file",
                issues);
    }

    private void cover(@NotNull TrackInfo ti, @NotNull List<String> issues) {
        if (shouldIgnore("cover")) {
            return;
        }

        file(ti.coverImageFile(),
                () -> missing(
                        ti,
                        f -> !f.equals(ti.backgroundImageFile()) && Utils.verifyImage(f),
                        Comparator.comparing(f -> f.getName().contains("[BG]") ? 1 : 0),
                        Utils.IMAGE_EXT
                ).orElse(null),
                ti::setCoverImageFileName,
                f -> ImageIO.read(ti.coverImageFile()),
                "Cover image",
                issues);
    }

    private void background(@NotNull TrackInfo ti, @NotNull List<String> issues) {
        if (shouldIgnore("background")) {
            return;
        }

        file(ti.backgroundImageFile(),
                () -> missing(
                        ti,
                        f -> !f.equals(ti.coverImageFile()) && Utils.verifyImage(f),
                        Comparator.comparing(f -> f.getName().contains("[CO]") ? 1 : 0),
                        Utils.IMAGE_EXT
                ).orElse(null),
                ti::setBackgroundImageFileName,
                f -> ImageIO.read(ti.backgroundImageFile()),
                "Background image",
                issues);
        issues.remove("Background image is missing.");
    }

    private void file(@Nullable File file,
                      @NotNull Supplier<File> finder,
                      @NotNull Consumer<String> setter,
                      @NotNull ThrowingConsumer<File> tester,
                      @NotNull String name,
                      @NotNull List<String> issues) {

        if (file != null) {
            try {
                tester.accept(file);
                return;
            } catch (Exception ignored) {
            }

            if (!dryRun) {
                issues.add(name + " is invalid -> UNSET");
                setter.accept(null);
            } else {
                issues.add(name + " is invalid.");
            }
        }

        if (!dryRun) {
            setter.accept(null);
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
        if (shouldIgnore("lyrics")) {
            return;
        }

        // LYRICS
        NoteLyricCollection nlc;
        try {
            nlc = ti.noteLyrics();
            int beat = nlc.noteLyricBlocks().getFirst().noteLyrics().getFirst().beat();
            if (beat < 0) {
                if (!dryRun) {
                    issues.add("First beat was negative -> FIXED");
                    nlc = nlc.shift(Math.abs(beat));
                } else {
                    issues.add("First beat is negative.");
                }
            }

            if (nlc.noteLyricBlocks().stream().anyMatch(nlb -> !nlb.isValid())) {
                issues.add("Lyrics contain invalid note blocks.");
            }

            long blocks = nlc.noteLyricBlocks().stream().filter(b -> b.singer() != null).count();
            if (ti.isDuet() && blocks < 2) {
                issues.add("Duet with less than 2 blocks.");
            }

            if (nlc.noteLyricBlocks().stream().flatMap(b -> b.noteLyrics().stream()).filter(nl -> nl.type() != NoteLyric.NoteType.BREAK).anyMatch(nl -> nl.duration() == 0)) {
                issues.add("Lyrics contains notes with duration 0.");
            }

            if (!dryRun) {
                ti.overwriteNoteLyrics(nlc);
            }
        } catch (Exception e) {
            issues.add("Error during lyrics validation: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private Optional<File> missing(TrackInfo ti, Predicate<File> filter, String... extensions) {
        return Utils.findFilesByExtensions(ti.parentDirectory(), extensions).stream()
                .filter(filter)
                .findFirst();
    }

    private Optional<File> missing(TrackInfo ti, Predicate<File> filter, Comparator<File> comparator, String... extensions) {
        return Utils.findFilesByExtensions(ti.parentDirectory(), extensions).stream()
                .filter(filter)
                .max(comparator);
    }


    private interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    private boolean shouldIgnore(String key) {
        return ignore != null && Arrays.stream(ignore).anyMatch(s -> s.equalsIgnoreCase(key));
    }

}
