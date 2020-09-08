package be.jorisg.ultrastarorganizer.exceptions;

public class LibraryException extends Exception {

    private final int code;

    public LibraryException(int code) {
        super();
        this.code = code;
    }

    public LibraryException(int code, String message) {
        super(message);
        this.code = code;
    }

    public LibraryException(int code,String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public LibraryException(int code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
