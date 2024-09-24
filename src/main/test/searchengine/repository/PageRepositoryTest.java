package searchengine.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class PageRepositoryTest {
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;

    @Test
    public void test1(){
        long size = 100;
        long begin = System.currentTimeMillis();
        List<Page> list = new ArrayList<>();
        Site sd = new Site();
        sd.setStatus(Status.INDEXING);
        sd.setLastError("");
        sd.setUrl("some url");
        sd.setStatusTime(LocalDateTime.now());
        sd.setName("Name");
        siteRepository.save(sd);

        for (int i = 0; i < size; i++) {
            Page pd = new Page();
            pd.setSite(sd);
            pd.setPath("path " + i);
            pd.setContent(String.valueOf(i));
            pd.setCode(200);
            list.add(pd);
        }

        pageRepository.saveAll(list);
        assertEquals(size, pageRepository.count());
        long end = System.currentTimeMillis();
        pageRepository.deleteAll(list);
        siteRepository.delete(sd);
        System.out.println("Времени прошло: " + (end - begin) + " мс");
    }
}
