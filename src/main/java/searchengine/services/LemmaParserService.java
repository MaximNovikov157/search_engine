package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Service;
import searchengine.dto.search.LemmaEntry;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class LemmaParserService {

    private final LuceneMorphology luceneMorph;
    private static final String REGEX_FOR_NORMALIZE = "[^а-яА-Я]";
    private static final Set<String> IGNORED_POS = Set.of("ЧАСТ", "МЕЖД", "СОЮЗ", "ПРЕДЛ");

    public LemmaParserService() throws IOException {
        this.luceneMorph = new RussianLuceneMorphology();
    }
    protected Map<String, Integer> parseLemmas(String content) {
        return getLemmaCounts(content).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().intValue(),
                        (e1, e2) -> e1,
                        HashMap::new
                ));
    }

    private Map<String, Long> getLemmaCounts(String text) {
        return Arrays.stream(splitIntoWords(text))
                .map(this::normalize)
                .filter(Predicate.not(String::isBlank))
                .filter(this::isNotParticle)
                .map(this::getFirstNormalForm)
                .filter(Predicate.not(String::isBlank))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private String[] splitIntoWords(String text) {
        return text.trim().split("\\s+");
    }

    String normalize(String word) {
        return word.toLowerCase().replaceAll(REGEX_FOR_NORMALIZE, "");
    }

    private boolean isNotParticle(String word) {
        return luceneMorph.getMorphInfo(word).stream()
                .map(this::extractPartOfSpeech)
                .noneMatch(IGNORED_POS::contains);
    }

    String getFirstNormalForm(String word) {
        List<String> normalForms = luceneMorph.getNormalForms(word);
        return normalForms.isEmpty() ? "" : normalForms.get(0);
    }

    private String extractPartOfSpeech(String morphInfo) {
        String[] parts = morphInfo.split("\\s+");
        return parts.length > 1 ? parts[1] : "";
    }

    public List<LemmaEntry> matchWordWithLemmas(String text) {
        List<LemmaEntry> lemmaEntries = new ArrayList<>();
        String[] words = splitIntoWords(text);
        for (int i = 0; i < words.length; i++) {
            String word = normalize(words[i]);
            if (!word.isBlank() && isNotParticle(word)) {
                lemmaEntries.add(new LemmaEntry(words[i], new HashSet<>(luceneMorph.getNormalForms(word)), i));
            }
        }
        return lemmaEntries;
    }
}