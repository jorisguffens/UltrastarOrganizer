package be.jorisg.ultrastarorganizer.commands.tracklist.generators;

import be.jorisg.ultrastarorganizer.domain.TrackInfo;

import java.io.File;
import java.util.List;

public interface TracklistGenerator {

    void generate(File output, List<TrackInfo> songs) throws Exception;

}
