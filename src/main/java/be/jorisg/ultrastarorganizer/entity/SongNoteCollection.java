package be.jorisg.ultrastarorganizer.entity;

import java.util.ArrayList;
import java.util.List;

public class SongNoteCollection {

    private final List<SongNote> notes = new ArrayList<>();

    public SongNoteCollection(List<String> lines) {
        for ( String line : lines ) {
            if ( line.startsWith("E") ) {
                break;
            }
            notes.add(new SongNote(line));
        }
    }

    public List<SongNote> getNotes() {
        return notes;
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
