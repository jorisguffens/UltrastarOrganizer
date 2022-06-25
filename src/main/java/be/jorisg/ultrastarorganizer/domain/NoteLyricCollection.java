/*
 * This file is part of Ultrastar Organizer, licensed under the MIT License.
 *
 * Copyright (c) Joris Guffens
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
                blocks.add(new NoteLyricBlock(block, singer));
                block.clear();

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
