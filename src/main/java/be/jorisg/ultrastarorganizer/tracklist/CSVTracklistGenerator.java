package be.jorisg.ultrastarorganizer.tracklist;

import be.jorisg.ultrastarorganizer.domain.TrackInfo;
import be.jorisg.ultrastarorganizer.entity.SongInfo;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class CSVTracklistGenerator implements TracklistGenerator {

    @Override
    public void generate(File output, List<TrackInfo> tracks) {
        try (
                PrintWriter pw = new PrintWriter(output);
        ) {
            pw.write("SEP=,\n");

            CSVPrinter printer = new CSVPrinter(pw, CSVFormat.DEFAULT.withHeader(
                    "Artist", "Title", "IsDuet", "HasCoverImage", "HasBackgroundImage", "HasVideo"));

            for (TrackInfo track: tracks) {
                printer.printRecord(
                        track.artist(),
                        track.title(),
                        track.isDuet(),
                        track.coverImagePath() != null,
                        track.backgroundImagePath() != null,
                        track.videoPath() != null
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
