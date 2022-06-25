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

import java.util.Arrays;
import java.util.regex.Pattern;

public record NoteLyric(NoteType type, int beat, int duration, int note, String text) {

    public NoteLyric withType(NoteType type) {
        return new NoteLyric(type, beat, duration, note, text);
    }

    public NoteLyric withBeat(int beat) {
        return new NoteLyric(type, beat, duration, note, text);
    }

    public NoteLyric withDuration(int duration) {
        return new NoteLyric(type, beat, duration, note, text);
    }

    public NoteLyric withNote(int note) {
        return new NoteLyric(type, beat, duration, note, text);
    }

    public NoteLyric withText(String text) {
        return new NoteLyric(type, beat, duration, note, text);
    }

    @Override
    public String toString() {
        if (type == NoteType.BREAK) {
            return type.key + " " + beat;
        }
        return type.key + " " + beat + " " + duration + " " + note + " " + text;
    }

    //

    public enum NoteType {
        NORMAL(":"), GOLDEN("*"), FREESTYLE("F"), BREAK("-");

        final String key;

        NoteType(String key) {
            this.key = key;
        }

        public static NoteType fromKey(String key) {
            return Arrays.stream(NoteType.values()).filter(n -> n.key.equals(key)).findFirst().orElse(null);
        }
    }

    //

    public static NoteLyric fromString(String str) {
        str = str.replaceAll(Pattern.quote("\t"), " ");
        str = str.stripLeading();
        String[] args = str.split(Pattern.quote(" "));

        if (args[0].length() > 1) {
            throw new IllegalArgumentException("Invalid note lyric line: '" + str + "'");
        }

        NoteType type = NoteType.fromKey(args[0]);
        if (type == null) {
            throw new IllegalArgumentException("Invalid note type for '" + str + "'");
        }

        int beat = Integer.parseInt(args[1]);
        if (type == NoteType.BREAK) {
            return new NoteLyric(type, beat, 0, 0, "");
        }

        int duration = Integer.parseInt(args[2]);
        int note = Integer.parseInt(args[3]);

        String prefix = type.key + " " + beat + " " + duration + " " + note + " ";
        String text = "";
        if (str.length() > prefix.length()) {
            text = str.substring(prefix.length());
        }

        return new NoteLyric(type, beat, duration, note, text);
    }
}
