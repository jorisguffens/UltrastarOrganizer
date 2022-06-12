package be.jorisg.ultrastarorganizer.tracklist;

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
