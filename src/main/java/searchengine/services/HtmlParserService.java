package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.AppConfig;
import searchengine.dto.index.PageFetchResult;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

@Service
@RequiredArgsConstructor
public class HtmlParserService {

    private final AppConfig appConfig;

    public String cleanHtml(String html) {
        return Jsoup.parse(html).text();
    }

    public PageFetchResult fetchDocumentWithStatus(String url) throws IOException {
        Connection connection = Jsoup.connect(url)
                .userAgent(appConfig.getUserAgent())
                .referrer(appConfig.getReferrer())
                .timeout(5000);

        Document document = connection.get();
        int statusCode = connection.response().statusCode();

        return new PageFetchResult(document, statusCode);
    }

    public String getTitle(String content) {
        Document document = Jsoup.parse(content);
        return document.title();
    }

    public String getPath(String url) throws MalformedURLException {
        URL parsedUrl = new URL(url);
        return parsedUrl.getPath();
    }
}