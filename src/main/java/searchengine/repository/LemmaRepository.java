package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    @Query("SELECT l FROM Lemma l WHERE l.site.id = :site_id")
    List<Lemma> findLemmaBySiteId(@Param("site_id") int site_id);

    @Query("SELECT l FROM Lemma l WHERE l.site.id = :site_id AND l.lemma =:lemma")
    List<Lemma> findLemmaBySiteIdAndLemma(@Param("site_id") int site_id, @Param("lemma") String lemma);
}
