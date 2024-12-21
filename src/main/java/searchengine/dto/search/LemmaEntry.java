package searchengine.dto.search;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.*;
import java.util.Objects;
import java.util.Set;

@Getter
@AllArgsConstructor
public class LemmaEntry {

    private final String lemma;
    private final Set<String> lemmaSet;
    private final int index;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LemmaEntry that = (LemmaEntry) o;
        return Objects.equals(lemma, that.lemma) ||
                !Collections.disjoint(lemmaSet, that.lemmaSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lemma, index, lemmaSet);
    }
}