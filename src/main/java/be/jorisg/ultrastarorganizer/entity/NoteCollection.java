package be.jorisg.ultrastarorganizer.entity;

import java.util.ArrayList;
import java.util.List;

public class NoteCollection {

    private final List<Note> notes = new ArrayList<>();

    public NoteCollection(List<String> lines) {
        for ( String line : lines ) {
            if ( line.startsWith("E") ) {
                break;
            }
            notes.add(new Note(line));
        }
    }

    public List<Note> getNotes() {
        return notes;
    }

    public void shift(int amount) {
        if ( notes.get(0).getBeat() + amount < 0 ) {
            throw new IllegalArgumentException();
        }

        for ( Note note : notes ) {
            note.setBeat(note.getBeat() + amount);
        }
    }

    public List<String> getLines() {
        List<String> lines = new ArrayList<>();
        for ( Note note :notes ) {
            lines.add(note.toString());
        }
        return lines;
    }
}
