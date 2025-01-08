package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.model.Site;
import searchengine.model.Status;

import java.util.List;

public interface SiteRepository extends JpaRepository<Site, Integer> {

    void deleteByUrl(String url);

    @Query("SELECT s FROM Site s WHERE :url LIKE CONCAT(s.url, '%')")
    Site findByUrlStartingWith(@Param("url") String url);

    List<Site> findByStatus(Status status);

    boolean existsByStatus(Status status);
}