package searchengine.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    Optional<Lemma> findByLemmaAndSite(String lemma, Site site);

    @Query("SELECT l FROM Lemma l WHERE l.site = :site")
    List<Lemma> findBySite(@Param("site") Site site, Pageable pageable);

    int countBySite(Site site);

    void deleteBySite(Site site);

}