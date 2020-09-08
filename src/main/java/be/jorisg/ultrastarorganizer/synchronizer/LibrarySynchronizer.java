package be.jorisg.ultrastarorganizer.synchronizer;

import be.jorisg.ultrastarorganizer.entity.SongNote;
import be.jorisg.ultrastarorganizer.entity.SongNoteCollection;
import be.jorisg.ultrastarorganizer.exceptions.InvalidSongInfoFileException;
import be.jorisg.ultrastarorganizer.entity.SongInfo;
import be.jorisg.ultrastarorganizer.transcribe.AudioStreamPublisher;
import be.jorisg.ultrastarorganizer.utils.Utils;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchProcessor;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import javazoom.jl.player.advanced.AdvancedPlayer;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.transcribestreaming.TranscribeStreamingAsyncClient;
import software.amazon.awssdk.services.transcribestreaming.model.*;

import javax.sound.sampled.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LibrarySynchronizer {

    private final File directory;

    private static final int MAX_NOTE_DEVIATION = 5;
    private static final int MAX_TIME_DEVIATION = 500; // milliseconds

    public LibrarySynchronizer(File directory) {
        if ( !directory.isDirectory() ) {
            throw new IllegalArgumentException("File is not a directory.");
        }
        this.directory = directory;
    }

    public void run(boolean update) {
        File[] files = directory.listFiles();
        for ( int i = 0; i < files.length; i++ ) {
            try {
                File songDir = files[i];
                System.out.println("Processing directory " + (i+1) + " of " + files.length + ": " + songDir.getName() + "");
                process(songDir, update);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("Done!");
    }

    private void process(File dir, boolean update) {
        List<File> txtFiles = Utils.getFilesByExtensions(dir, "txt");
        if ( txtFiles.isEmpty() ) {
            System.out.println("No song info (.txt) found.");
            return;
        }

        List<SongInfo> songInfos = new ArrayList<>();
        for ( File file : txtFiles ) {
            try {
                SongInfo info = new SongInfo(file);
                songInfos.add(info);
            } catch (InvalidSongInfoFileException e) {}
        }

        if ( songInfos.isEmpty() ) {
            System.out.println("No valid song info files (.txt) found.");
            return;
        }

        SongInfo mainInfo = songInfos.stream()
                .filter(s -> !s.isDuet())
                .findFirst()
                .orElse(songInfos.get(0));

        SongNoteCollection songNotes = new SongNoteCollection(mainInfo.getNotes());

        int gap = mainInfo.containsHeader("gap") ? (int) Float.parseFloat(mainInfo.getHeaderValue("gap").replace(",", ".")) : 0;
        float bpm = Float.parseFloat(mainInfo.getHeaderValue("bpm").replace(",","."));
        float beatDuration = 60 * 1000f / bpm;
        int maxBeatDeviation = Math.round((MAX_TIME_DEVIATION / beatDuration));

        SongNote first = songNotes.getNotes().get(0);

        File audioFile = songInfos.get(0).getMP3();
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(audioFile)) {
            AudioFormat sourceFormat = ais.getFormat();
            AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(), sourceFormat.getChannels()*2, sourceFormat.getSampleRate(), true);

            AudioInputStream cais = AudioSystem.getAudioInputStream(targetFormat, ais);
            JVMAudioInputStream audioStream = new JVMAudioInputStream(cais);

            Map<Integer, Integer> measuredNotes = new HashMap<>();

            int bufferSize = 1024;
            int overlap = 0;
            AudioDispatcher dispatcher = new AudioDispatcher(audioStream, bufferSize, overlap);
            dispatcher.addAudioProcessor(new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.MPM, targetFormat.getSampleRate(), bufferSize, (pitchDetectionResult, audioEvent) -> {
                if ( pitchDetectionResult.getPitch() == -1 ) {
                    return;
                }
                if ( pitchDetectionResult.getProbability() < 0.80 ) {
                    return;
                }

                double timeStamp = audioEvent.getTimeStamp();
                float pitch = pitchDetectionResult.getPitch();
                float probability = pitchDetectionResult.getProbability();
                double rms = audioEvent.getRMS() * 100;
                int beat = (int) (((timeStamp * 1000) - gap) / beatDuration);

                double log2 = Math.log(pitch / 440) / Math.log(2);
                int note = (int) (69 + 12 * log2) - 33;
                measuredNotes.put(beat, note);

                //System.out.println(String.format("Pitch detected at %.2fs: %.2fHz (%.2f probability, RMS: %.5f), note %d, beat %d", timeStamp, pitch, probability, rms, note, beat));
            }));
            dispatcher.run();

            int totalCheckedNotes = 0;
            List<SongNote> correctNotes = new ArrayList<>();

            for ( int beat : measuredNotes.keySet().stream().sorted().collect(Collectors.toList()) ) {
                SongNote songNote = songNotes.getNoteAtBeat(beat, maxBeatDeviation);
                if ( songNote == null || correctNotes.contains(songNote) ) {
                    continue;
                }
                totalCheckedNotes++;

                int note = measuredNotes.get(beat);
                int noteDeviation = Math.abs(songNote.getNote() - note);
                if ( noteDeviation > MAX_NOTE_DEVIATION ) {
                    //System.out.println(String.format("WRONG note %d, beat %d - %d (measured note %d, beat %d)", songNote.getNote(), songNote.getBeat(), songNote.getBeat() + songNote.getLength(), note, beat));
                    continue;
                }

                correctNotes.add(songNote);
            }

            float accuracy = correctNotes.size() / (float) totalCheckedNotes;
            System.out.println(String.format("Total accuracy: %d/%d (%.2f)", correctNotes.size(), totalCheckedNotes, accuracy));

        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }

    }

}
