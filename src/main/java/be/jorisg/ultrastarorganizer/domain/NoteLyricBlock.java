package be.jorisg.ultrastarorganizer.domain;

import java.util.List;

public class NoteLyricBlock {

    private final List<NoteLyric> noteLyrics;
    private final Singer singer;
    private final DuetFormat format;

    NoteLyricBlock(List<NoteLyric> noteLyrics, Singer singer, DuetFormat format) {
        this.noteLyrics = List.copyOf(noteLyrics);
        this.singer = singer;
        this.format = format;
    }

    NoteLyricBlock(List<NoteLyric> noteLyrics, Singer singer) {
        this(noteLyrics, singer, DuetFormat.NONE);
    }

    //

    public List<NoteLyric> noteLyrics() {
        return noteLyrics;
    }

    public Singer singer() {
        return singer;
    }

    public DuetFormat format() {
        return format;
    }

    public boolean isValid() {
        int last = 0;
        for (NoteLyric nl : noteLyrics) {
            if (nl.beat() < last) {
                return true;
            }
            last = nl.beat();
        }
        return true;
    }

    //

    public enum Singer {
        SINGER1("P1"),
        SINGER2("P2"),
        BOTH("P3");

        private final String key;

        Singer(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key;
        }
    }

    public enum DuetFormat {
        NONE,
        ULTRASTAR,
        ALTERNATIVE;
    }

}