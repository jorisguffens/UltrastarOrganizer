package be.jorisg.ultrastarorganizer.entity;

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
            notes.add(new SongNote(line));
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
