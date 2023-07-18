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
        NORMAL(":"), GOLDEN("*"), FREESTYLE("F"), BREAK("-"), RAP("R"), GOLDEN_RAP("G");

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
            if ( !args[0].startsWith("-") ) {
                throw new IllegalArgumentException("Invalid note lyric line: '" + str + "'. ");
            }
            int beat = Integer.parseInt(args[0].substring(1));
            return new NoteLyric(NoteType.BREAK, beat, 0, 0, "");
        }

        NoteType type = NoteType.fromKey(args[0]);
        if (type == null) {
            throw new IllegalArgumentException("Invalid note lyric line: '" + str + "'. Unknown note type: " + args[0] + ".");
        }

        int beat;
        try {
            beat = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid note lyric line: '" + str + "'. Invalid beat: " + args[1] + ".");
        }
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
