package be.jorisg.ultrastarorganizer.domain;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

public record NoteLyricCollection(List<NoteLyricBlock> noteLyricBlocks) {

    public NoteLyricCollection(List<NoteLyricBlock> noteLyricBlocks) {
        this.noteLyricBlocks = List.copyOf(noteLyricBlocks);
    }

    public NoteLyricCollection shift(int amount) {
        if (noteLyricBlocks.get(0).noteLyrics().get(0).beat() + amount < 0) {
            throw new IllegalArgumentException();
        }

        List<NoteLyricBlock> blocks = new ArrayList<>();
        for ( NoteLyricBlock block : noteLyricBlocks ) {
            List<NoteLyric> noteLyrics = new ArrayList<>();
            for (NoteLyric note : block.noteLyrics) {
                noteLyrics.add(note.withBeat(note.beat() + amount));
            }
            blocks.add(new NoteLyricBlock(noteLyrics, block.singer));
        }

        return new NoteLyricCollection(blocks);
    }

    public List<String> toStringList() {
        List<String> lines = new ArrayList<>();
        for (NoteLyricBlock block : noteLyricBlocks) {
            if ( block.singer() != null ) {
                lines.add(block.singer().toString());
            }
            for ( NoteLyric noteLyric : block.noteLyrics() ) {
                lines.add(noteLyric.toString());
            }
        }
        lines.add("E");
        return lines;
    }

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

    public static class NoteLyricBlock {

        private final List<NoteLyric> noteLyrics;
        private final Singer singer;

        private NoteLyricBlock(List<NoteLyric> noteLyrics, Singer singer) {
            this.noteLyrics = List.copyOf(noteLyrics);
            this.singer = singer;
        }

        private NoteLyricBlock(List<NoteLyric> noteLyrics) {
            this(noteLyrics, null);
        }

        public List<NoteLyric> noteLyrics() {
            return noteLyrics;
        }

        public Singer singer() {
            return singer;
        }

        public NoteLyricBlock withSinger(Singer singer) {
            return new NoteLyricBlock(noteLyrics, singer);
        }

        public boolean isValid() {
            int last = 0;
            for ( NoteLyric nl : noteLyrics ) {
                if ( nl.beat() < last ) {
                    return true;
                }
                last = nl.beat();
            }
            return true;
        }
    }

    //

    public static NoteLyricCollection fromStringList(List<String> noteLyrics) {
        List<NoteLyricBlock> blocks = new ArrayList<>();
        List<NoteLyric> block = new ArrayList<>();
        Singer singer = null;
        for (String line : noteLyrics) {
            if (line.startsWith("E")) {
                break;
            }

            String trim = line.trim();
            if (trim.equals("")) {
                continue;
            }

            // duet stuff
            if ( trim.startsWith("P") ) {
                if ( !block.isEmpty() ) {
                    blocks.add(new NoteLyricBlock(block, singer));
                    block.clear();
                }

                if (trim.charAt(1) == '1') {
                    singer = Singer.SINGER1;
                }
                else if (trim.charAt(1) == '2') {
                    singer = Singer.SINGER2;
                } else {
                    singer = Singer.BOTH;
                }

                continue;
            }

            block.add(NoteLyric.fromString(line));
        }

        blocks.add(new NoteLyricBlock(block, singer));
        return new NoteLyricCollection(blocks);
    }
}
