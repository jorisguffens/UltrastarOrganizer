package be.jorisg.ultrastarorganizer.commands.tracklist.generators;

import be.jorisg.ultrastarorganizer.domain.TrackInfo;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.Normalizer;
import java.util.List;

public class CSVTracklistGenerator implements TracklistGenerator {

    @Override
    public void generate(File output, List<TrackInfo> tracks) {
        try (
                PrintWriter pw = new PrintWriter(output);
        ) {
            pw.write("SEP=,\n");

            CSVPrinter printer = new CSVPrinter(pw, CSVFormat.DEFAULT.withHeader(
                    "Artist", "Title", "Year", "Genre", "IsDuet", "HasCoverImage", "HasBackgroundImage", "HasVideo"));

            for (TrackInfo track : tracks) {
                String artist = ascii(track.artist());
                String title = ascii(track.title());

                printer.printRecord(
                        artist,
                        title,
                        track.header("YEAR").orElse(""),
                        track.header("GENRE").orElse(""),
                        track.isDuet(),
                        track.coverImageFile() != null,
                        track.backgroundImageFile() != null,
                        track.videoFile() != null
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private String ascii(String str) {
        str = Normalizer.normalize(str, Normalizer.Form.NFKD);
        str = str.replaceAll("[^\\p{ASCII}]", "");
        return str;
    }

}
