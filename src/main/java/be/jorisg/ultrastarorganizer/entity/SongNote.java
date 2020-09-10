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
