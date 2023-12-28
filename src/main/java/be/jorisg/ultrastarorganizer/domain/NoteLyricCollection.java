package be.jorisg.ultrastarorganizer.domain;

import java.util.ArrayList;
import java.util.List;

public record NoteLyricCollection(List<NoteLyricBlock> noteLyricBlocks) {

    public NoteLyricCollection(List<NoteLyricBlock> noteLyricBlocks) {
        this.noteLyricBlocks = List.copyOf(noteLyricBlocks);
    }

    public NoteLyricCollection shift(int amount) {
        if (noteLyricBlocks.getFirst().noteLyrics().getFirst().beat() + amount < 0) {
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

    public static NoteLyricCollection fromStringList(List<String> noteLyrics, boolean relative) {
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
            if (trim.isEmpty()) {
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
            if (!relative && noteLyric.beat() + 200 < beat) {
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

            if (!block.isEmpty()) {
                NoteLyric last = block.getLast();

                // Remove duplicate break blocks
                if (noteLyric.type() == NoteLyric.NoteType.BREAK
                        && last.type() == NoteLyric.NoteType.BREAK) {
                    block.removeLast();
                }

                // Fix format where 0 duration means until next beat
                if (noteLyric.type() != NoteLyric.NoteType.BREAK && last.duration() == 0) {
                    block.removeLast();
                    block.add(last.withDuration(noteLyric.beat() - last.beat()));
                }
            }

            if (relative && noteLyric.type() == NoteLyric.NoteType.BREAK && noteLyric.duration() == 0) {
                noteLyric = noteLyric.withBeat(noteLyric.beat() - 1).withDuration(1);
            }

            block.add(noteLyric);
            beat = noteLyric.beat();
        }

        blocks.add(new NoteLyricBlock(block, singer, format));
        return new NoteLyricCollection(blocks);
    }

}
