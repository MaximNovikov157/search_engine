package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.Site;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    void deleteBySite(Site site);

    int countBySite(Site site);

    int countAllBySite(Site site);

    boolean existsBySiteAndPath(Site site, String path);

    Optional<Page> findBySiteAndPath(Site site, String path);

}