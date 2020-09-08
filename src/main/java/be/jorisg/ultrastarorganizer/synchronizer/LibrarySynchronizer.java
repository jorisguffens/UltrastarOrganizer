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
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.transcribestreaming.TranscribeStreamingAsyncClient;
import software.amazon.awssdk.services.transcribestreaming.model.*;

import javax.sound.sampled.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LibrarySynchronizer {

    private final File directory;
    private final TranscribeStreamingAsyncClient transcribeStreamingClient;

    public LibrarySynchronizer(File directory) {
        if ( !directory.isDirectory() ) {
            throw new IllegalArgumentException("File is not a directory.");
        }
        this.directory = directory;

        transcribeStreamingClient = TranscribeStreamingAsyncClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(new DefaultAwsRegionProviderChain().getRegion())
                .build();
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

        for ( SongInfo info : songInfos ) {
            int gap = (int) Float.parseFloat(info.getHeaderValue("gap").replace(",","."));
            float bpm = Float.parseFloat(info.getHeaderValue("bpm").replace(",","."));
            int beat_duration = (int) (60 * 1000 / bpm);

            System.out.println("BPM: " + bpm + " (" + beat_duration + "ms)");
            System.out.println("Gap: " + gap + "ms (" + String.format("%.2f", gap/1000.0) + "s)");

            SongNoteCollection collection = new SongNoteCollection(info.getNotes());
            SongNote first = collection.getNotes().get(0);
            int offset = beat_duration * first.getBeat();
            System.out.println("First: " + first.getBeat() + " - " + offset + "ms (" + String.format("%.2f", offset/1000.0) + "s)");

            int totaloffset = gap + offset;
            System.out.println("Calculated: " + (totaloffset) + "ms (" + String.format("%.2f", totaloffset/1000.0) + "s)");
        }

        File audioFile = songInfos.get(0).getMP3();

        try (AudioInputStream ais = AudioSystem.getAudioInputStream(audioFile)) {
            AudioFormat sourceFormat = ais.getFormat();
            AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(), sourceFormat.getChannels()*2, sourceFormat.getSampleRate(), true);

            AudioInputStream cais = AudioSystem.getAudioInputStream(targetFormat, ais);
            JVMAudioInputStream audioStream = new JVMAudioInputStream(cais);

            int bufferSize = 4096;
            int overlap = 0;
            AudioDispatcher dispatcher = new AudioDispatcher(audioStream, bufferSize, overlap);

            dispatcher.addAudioProcessor(new AudioPlayer(targetFormat));

            dispatcher.addAudioProcessor(new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.MPM, targetFormat.getSampleRate(), bufferSize, (pitchDetectionResult, audioEvent) -> {
                if ( pitchDetectionResult.getPitch() == -1){
                    return;
                }

                double timeStamp = audioEvent.getTimeStamp();
                float pitch = pitchDetectionResult.getPitch();
                float probability = pitchDetectionResult.getProbability();
                double rms = audioEvent.getRMS() * 100;

                System.out.println(String.format("Pitch detected at %.2fs: %.2fHz ( %.2f probability, RMS: %.5f )", timeStamp,pitch,probability,rms));

                double log2 = Math.log(pitch / 440) / Math.log(2);
                int note = (int) (69 + 12 * log2) - 33;
                System.out.println("Note: " + note);

            }));


            dispatcher.run();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }

        if ( true ) {
            return;
        }


        try (AudioInputStream ais = AudioSystem.getAudioInputStream(audioFile)) {
            AudioFormat format = ais.getFormat();
            AudioStreamPublisher requestStream = new AudioStreamPublisher(ais);

//            AdvancedPlayer player = new AdvancedPlayer(ais,
//                    javazoom.jl.player.FactoryRegistry.systemRegistry().createAudioDevice());
//
//            player.play();

            // create request
            StartStreamTranscriptionRequest request = StartStreamTranscriptionRequest.builder()
                    .languageCode(LanguageCode.EN_US.toString())
                    .mediaEncoding(MediaEncoding.PCM)
                    .mediaSampleRateHertz((int) format.getSampleRate())
                    .build();

            System.out.println("Checking audio file...");

            // create response handler
            StartStreamTranscriptionResponseHandler response = getResponseHandler();

            // start stream
            transcribeStreamingClient.startStreamTranscription(request, requestStream, response).join();
        } catch (IOException | UnsupportedAudioFileException e) {
            e.printStackTrace();
        }
    }

    private StartStreamTranscriptionResponseHandler getResponseHandler() {
        return StartStreamTranscriptionResponseHandler.builder()
                .onResponse(r -> {
                    System.out.println("Received Initial response");
                })
                .onError(e -> {
                    System.out.println(e.getMessage());
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    System.out.println("Error Occurred: " + sw.toString());
                })
                .onComplete(() -> {
                    System.out.println("=== All records stream successfully ===");
                })
                .subscriber(event -> {
                    List<Result> results = ((TranscriptEvent) event).transcript().results();
                    if (results.size() > 0) {
                        if (!results.get(0).alternatives().get(0).transcript().isEmpty()) {
                            System.out.println(results.get(0).alternatives().get(0).transcript());
                        }
                    }
                })
                .build();
    }

}
