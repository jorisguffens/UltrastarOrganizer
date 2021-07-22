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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SongNoteCollection {

    private final List<SongNote> notes = new ArrayList<>();

    public SongNoteCollection(List<String> lines) {
        for ( String line : lines ) {
            if ( line.startsWith("E") ) {
                break;
            }
            if ( line.trim().equals("") ) {
                continue;
            }
            try {
                notes.add(new SongNote(line));
            } catch (InvalidSongNoteException ignored) {
            } catch (Exception ex) {
                System.out.println(line);
                throw ex;
            }
        }
    }

    public List<SongNote> getNotes() {
        return notes;
    }

    public List<SongNote> getRealNotes() {
        return notes.stream().filter(n -> n.getType() != SongNote.NoteType.BREAK).collect(Collectors.toList());
    }

    public SongNote getNoteAtBeat(int beat, int maxDeviation) {
        SongNote bestMatch = null;
        for ( SongNote n : getRealNotes() ) {
            if ( beat > n.getBeat() && beat < n.getBeat() + n.getLength() ) {
                return n;
            } else if ( beat > n.getBeat() - maxDeviation && beat < n.getBeat() + n.getLength() + maxDeviation ) {
                bestMatch = n;
            }
        }
        return bestMatch;
    }

    public void shift(int amount) {
        if ( notes.get(0).getBeat() + amount < 0 ) {
            throw new IllegalArgumentException();
        }

        for ( SongNote note : notes ) {
            note.setBeat(note.getBeat() + amount);
        }
    }

    public List<String> getLines() {
        List<String> lines = new ArrayList<>();
        for ( SongNote note :notes ) {
            lines.add(note.toString());
        }
        return lines;
    }
}
