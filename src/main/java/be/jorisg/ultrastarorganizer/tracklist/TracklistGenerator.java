package be.jorisg.ultrastarorganizer.tracklist;

import be.jorisg.ultrastarorganizer.entity.SongInfo;

import java.io.File;
import java.util.List;

public interface TracklistGenerator {

    void generate(File output, List<SongInfo> songs) throws Exception;

}
