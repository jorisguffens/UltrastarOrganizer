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

package be.jorisg.ultrastarorganizer.entity;

import be.jorisg.ultrastarorganizer.exceptions.InvalidSongNoteException;

import java.util.Arrays;
import java.util.regex.Pattern;

public class SongNote {

    private NoteType type;

    private int beat;

    private int length;
    private int note;
    private String text;

    public SongNote(String line) throws InvalidSongNoteException {
        String[] args = line.split(Pattern.quote(" "));
        type = NoteType.fromKey(args[0]);
        if ( type == null ) {
            throw new InvalidSongNoteException("Invalid type");
        }

        beat = Integer.parseInt(args[1]);
        if ( type == NoteType.BREAK ) {
            return;
        }

        length = Integer.parseInt(args[2]);
        note = Integer.parseInt(args[3]);

        String prefix = type.key + " " + beat + " " + length + " " + note + " ";
        if ( line.length() != prefix.length()-1 ) {
            text = line.substring(prefix.length());
        } else {
            text = "";
        }
    }

    @Override
    public String toString() {
        if ( type == NoteType.BREAK ) {
            return type.key + " " + beat;
        }
        return type.key + " " + beat + " " + length + " " + note + " " + text;
    }

    public NoteType getType() {
        return type;
    }

    public void setType(NoteType type) {
        this.type = type;
    }

    public int getBeat() {
        return beat;
    }

    public void setBeat(int beat) {
        this.beat = beat;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getNote() {
        return note;
    }

    public void setNote(int note) {
        this.note = note;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public enum NoteType {
        NORMAL(":"), GOLDEN("*"), FREESTYLE("F"), BREAK("-");

        String key;

        NoteType(String key) {
            this.key = key;
        }

        public static NoteType fromKey(String key) {
            return Arrays.stream(NoteType.values()).filter(n -> n.key.equals(key)).findFirst().orElse(null);
        }
    }
}
