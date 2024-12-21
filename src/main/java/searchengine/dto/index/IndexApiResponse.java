package searchengine.dto.index;

public class IndexApiResponse {

    private final boolean result;
    private final String error;

    public IndexApiResponse (boolean result, String error) {
        this.result = result;
        this.error = error;
    }

    public boolean isResult() {
        return result;
    }

    public String getError() {
        return error;
    }
}