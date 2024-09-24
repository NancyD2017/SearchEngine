package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {
    @Query("SELECT i FROM Index i WHERE i.page.id = :page_id")
    List<Index> findIndexesByPageId(@Param("page_id") int page_id);

    @Query("SELECT i FROM Index i WHERE i.id = :id")
    Index findIndexByIndexId(@Param("id") int id);
}
