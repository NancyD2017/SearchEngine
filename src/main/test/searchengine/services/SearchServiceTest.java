package searchengine.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import searchengine.dto.indexing.SearchResponse;

@SpringBootTest
public class SearchServiceTest {
    @Autowired
    private SearchService searchService;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Test
    public void testSearchServiceTime(){ //playback.ru за 10с находит 89 результатов
        long begin = System.currentTimeMillis();

        SearchService searchService = new SearchService();
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = transactionManager.getTransaction(def);
        SearchResponse sr = searchService.search("чехол", null, 0, 20);
        assert(sr.getError() == null);
        assert(sr.getCount() > 0);
        assert(sr.getData().size() > 0);
        System.out.println(sr.getCount());
        System.out.println(System.currentTimeMillis() - begin + " мс");
    }
}
