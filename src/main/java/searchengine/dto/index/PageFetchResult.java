package searchengine.dto.index;

import org.jsoup.nodes.Document;

public class PageFetchResult {

    private final Document document;
    private final int statusCode;

    public PageFetchResult(Document document, int statusCode) {
        this.document = document;
        this.statusCode = statusCode;
    }

    public Document getDocument() {
        return document;
    }

    public int getStatusCode() {
        return statusCode;
    }
}