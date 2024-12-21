package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface IndexRepository extends JpaRepository<Index, Integer> {
    void deleteByLemmaSite(Site site);

    void deleteAllByPage(Page page);

    List<Index> findAllByPage(Page page);

    Optional<Index> findByLemmaAndPage(Lemma lemma, Page page);

    Set<Index> findByLemmaAndPageIn(Lemma lemma, Set<Page> pages);

    Set<Index> findByPageAndLemmaIn(Page page, Set<Lemma> lemmas);

}