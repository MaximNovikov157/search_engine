package searchengine.exception;

public class SearchException extends RuntimeException {
    private final String message;

    public SearchException(String message) {
        super(message);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}