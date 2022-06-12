package be.jorisg.ultrastarorganizer.tracklist;

import be.jorisg.ultrastarorganizer.entity.SongInfo;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class CSVTracklistGenerator implements TracklistGenerator {

    @Override
    public void generate(File output, List<SongInfo> songs) {
        try (
                PrintWriter pw = new PrintWriter(output);
        ) {
            pw.write("SEP=,\n");

            CSVPrinter printer = new CSVPrinter(pw, CSVFormat.DEFAULT.withHeader(
                    "Artist", "Title", "IsDuet", "HasCover", "HasBackground", "HasVideo"));

            for (SongInfo info : songs) {
                printer.printRecord(
                        info.getArtist(),
                        info.getTitle(),
                        info.isDuet(),
                        info.getCover() != null,
                        info.getBackground() != null,
                        info.getVideo() != null
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
