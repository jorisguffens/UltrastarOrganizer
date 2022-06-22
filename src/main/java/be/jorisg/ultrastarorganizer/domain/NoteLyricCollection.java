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

public record NoteLyricCollection(List<NoteLyric> noteLyrics) {

    public NoteLyricCollection(List<NoteLyric> noteLyrics) {
        this.noteLyrics = List.copyOf(noteLyrics);
    }

//    public List<NoteLyric> realNoteLyrics() {
//        return noteLyrics.stream().filter(n -> n.type() != NoteLyric.NoteType.BREAK).collect(Collectors.toList());
//    }
//
//    public NoteLyric noteAtBeat(int beat, int maxDeviation) {
//        NoteLyric bestMatch = null;
//        for (NoteLyric n : realNoteLyrics()) {
//            if (beat > n.beat() && beat < n.beat() + n.length()) {
//                return n;
//            } else if (beat > n.beat() - maxDeviation && beat < n.beat() + n.length() + maxDeviation) {
//                bestMatch = n;
//            }
//        }
//        return bestMatch;
//    }

    public NoteLyricCollection shift(int amount) {
        if (noteLyrics.get(0).beat() + amount < 0) {
            throw new IllegalArgumentException();
        }

        List<NoteLyric> shifted = new ArrayList<>();
        for (NoteLyric note : noteLyrics) {
            shifted.add(note.withBeat(note.beat() + amount));
        }

        return new NoteLyricCollection(shifted);
    }

    public List<String> toStringList() {
        List<String> lines = new ArrayList<>();
        for (NoteLyric note : noteLyrics) {
            lines.add(note.toString());
        }
        return lines;
    }

    //

    public static NoteLyricCollection fromStringList(List<String> noteLyrics) {
        List<NoteLyric> collection = new ArrayList<>();
        for (String line : noteLyrics) {
            if (line.startsWith("E")) {
                break;
            }
            if (line.trim().equals("")) {
                continue;
            }

            collection.add(NoteLyric.fromString(line));
        }

        return new NoteLyricCollection(collection);
    }
}
