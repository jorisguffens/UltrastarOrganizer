package be.jorisg.ultrastarorganizer.exceptions;

public class InvalidSongInfoFileException extends Exception {

    public InvalidSongInfoFileException() {
        super();
    }

    public InvalidSongInfoFileException(String message) {
        super(message);
    }

    public InvalidSongInfoFileException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidSongInfoFileException(Throwable cause) {
        super(cause);
    }

}
