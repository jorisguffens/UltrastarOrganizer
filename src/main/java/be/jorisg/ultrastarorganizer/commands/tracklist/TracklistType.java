package be.jorisg.ultrastarorganizer.commands.tracklist;

import be.jorisg.ultrastarorganizer.commands.tracklist.generators.CSVTracklistGenerator;
import be.jorisg.ultrastarorganizer.commands.tracklist.generators.ODTTracklistGenerator;
import be.jorisg.ultrastarorganizer.commands.tracklist.generators.TracklistGenerator;

public enum TracklistType {
    CSV(new CSVTracklistGenerator()),
    ODT(new ODTTracklistGenerator());

    private final TracklistGenerator generator;

    TracklistType(TracklistGenerator generator) {
        this.generator = generator;
    }

    public final TracklistGenerator generator() {
        return generator;
    }
}
