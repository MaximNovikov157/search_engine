package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.search.LemmaEntry;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SnippetBuilderService {

    private final LemmaParserService lemmaParserService;
    private final HtmlParserService htmlParserService;

    private static final int SNIPPET_SYMBOLS_SIZE = 200;


    public String generateSnippet(String searchQuery, String pageContent) {
        List<LemmaEntry> searchQueryLemmas = lemmaParserService.matchWordWithLemmas(searchQuery);
        List<LemmaEntry> pageContentLemmas = lemmaParserService.matchWordWithLemmas(htmlParserService.cleanHtml(pageContent));
        List<LemmaEntry> commonLemmas = findLongestCommonSubsequence(searchQueryLemmas, pageContentLemmas);
        return buildSnippet(pageContent, commonLemmas);
    }

    private String buildSnippet(String rawContent, List<LemmaEntry> lemmaSequence) {
        String[] contentWords = htmlParserService.cleanHtml(rawContent).trim().split("\\s+");

        int snippetStartIndex = lemmaSequence.get(0).getIndex();
        int snippetEndIndex = lemmaSequence.get(lemmaSequence.size() - 1).getIndex() + 1;

        int snippetLeftIndex = snippetStartIndex;
        int snippetRightIndex = snippetEndIndex;
        int snippetLength = 0;

        while (snippetLength < SNIPPET_SYMBOLS_SIZE) {
            if (snippetLeftIndex > 0) {
                snippetLeftIndex--;
                snippetLength += contentWords[snippetLeftIndex].length() + 1;
            }

            if (snippetRightIndex < contentWords.length) {
                snippetLength += contentWords[snippetRightIndex].length() + 1;
                snippetRightIndex++;
            }

            if (snippetStartIndex == 0 && snippetEndIndex == contentWords.length) {
                break;
            }
        }

        return createSnippetWithHighlights(contentWords, snippetLeftIndex, snippetStartIndex, snippetEndIndex, snippetRightIndex);
    }


    private String createSnippetWithHighlights(String[] words, int leftIndex, int startIndex, int endIndex, int rightIndex) {
        StringBuilder snippet = new StringBuilder();

        for (int i = leftIndex; i < startIndex; i++) {
            snippet.append(words[i]).append(" ");
        }

        for (int i = startIndex; i < endIndex; i++) {
            snippet.append("<b>").append(words[i]).append("</b>").append(" ");
        }

        for (int i = endIndex; i < rightIndex; i++) {
            snippet.append(words[i]).append(" ");
        }

        return snippet.toString().trim();
    }

    private List<LemmaEntry> findLongestCommonSubsequence(List<LemmaEntry> query, List<LemmaEntry> page) {
        int maxLength = 0;
        int maxJ = 0;
        int[] current = new int[page.size()];

        for (int i = 0; i < query.size(); i++) {
            int prev = 0;

            for (int j = 0; j < page.size(); j++) {
                int temp = current[j];

                if (query.get(i).equals(page.get(j))) {
                    if (i == 0 || j == 0) {
                        current[j] = 1;
                    } else {
                        current[j] = prev + 1;
                    }

                    if (current[j] > maxLength) {
                        maxLength = current[j];
                        maxJ = j;
                    }
                } else {
                    current[j] = 0;
                }

                prev = temp;
            }
        }

        return page.subList(maxJ - maxLength + 1, maxJ + 1);
    }

}