package be.jorisg.ultrastarorganizer.entity;

import java.util.Arrays;
import java.util.regex.Pattern;

public class SongNote {

    private NoteType type;

    private int beat;

    private int length;
    private int tone;
    private String text;

    public SongNote(String line) {
        String[] args = line.split(Pattern.quote(" "));
        type = NoteType.fromKey(args[0]);
        beat = Integer.parseInt(args[1]);
        if ( type == NoteType.BREAK ) {
            return;
        }

        length = Integer.parseInt(args[2]);
        tone = Integer.parseInt(args[3]);
        text = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
    }

    @Override
    public String toString() {
        if ( type == NoteType.BREAK ) {
            return type.key + " " + beat;
        }
        return type.key + " " + beat + " " + length + " " + tone + " " + text;
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

    public int getTone() {
        return tone;
    }

    public void setTone(int tone) {
        this.tone = tone;
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
