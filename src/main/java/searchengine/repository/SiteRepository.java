package searchengine.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;


@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {
    @Query("SELECT s FROM Site s WHERE s.name = :siteName AND s.url = :url")
    Site findSiteByNameAndUrl(@Param("siteName") String siteName, @Param("url") String url);

    @Query("SELECT s FROM Site s WHERE s.url = :url")
    Site findSiteByUrl(@Param("url") String url);
}
