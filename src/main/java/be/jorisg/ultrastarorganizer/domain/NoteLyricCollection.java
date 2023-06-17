package be.jorisg.ultrastarorganizer.domain;

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
            for (NoteLyric note : block.noteLyrics()) {
                noteLyrics.add(note.withBeat(note.beat() + amount));
            }
            blocks.add(new NoteLyricBlock(noteLyrics, block.singer()));
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

    public static NoteLyricCollection fromStringList(List<String> noteLyrics) {
        List<NoteLyricBlock> blocks = new ArrayList<>();
        List<NoteLyric> block = new ArrayList<>();

        NoteLyricBlock.Singer singer = null;
        NoteLyricBlock.DuetFormat format = NoteLyricBlock.DuetFormat.NONE;
        int beat = Integer.MIN_VALUE;

        for (String line : noteLyrics) {
            if (line.startsWith("E")) {
                break;
            }

            String trim = line.trim();
            if (trim.equals("")) {
                continue;
            }

            // duet (ultrastar format)
            if (trim.startsWith("P")) {
                format = NoteLyricBlock.DuetFormat.ULTRASTAR;
                if (!block.isEmpty()) {
                    blocks.add(new NoteLyricBlock(block, singer, format));
                    block.clear();
                }

                if (trim.charAt(1) == '1') {
                    singer = NoteLyricBlock.Singer.SINGER1;
                } else if (trim.charAt(1) == '2') {
                    singer = NoteLyricBlock.Singer.SINGER2;
                } else {
                    singer = NoteLyricBlock.Singer.BOTH;
                }

                continue;
            }

            NoteLyric noteLyric = NoteLyric.fromString(line);

            // duet (alternative format)
            if (noteLyric.beat() + 200 < beat) {
                // first block
                if ( blocks.isEmpty() ) {
                    format = NoteLyricBlock.DuetFormat.ALTERNATIVE;
                    singer = NoteLyricBlock.Singer.SINGER1;
                }

                blocks.add(new NoteLyricBlock(block, singer, format));
                block.clear();

                // go to next singer
                singer = NoteLyricBlock.Singer.values()[Math.min(singer.ordinal() + 1, NoteLyricBlock.Singer.values().length - 1)];
            }

            block.add(noteLyric);
            beat = noteLyric.beat();
        }

        blocks.add(new NoteLyricBlock(block, singer, format));
        return new NoteLyricCollection(blocks);
    }

}
