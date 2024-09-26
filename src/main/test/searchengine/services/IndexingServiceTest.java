package searchengine.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.Status;
import searchengine.repository.*;

import java.util.List;

@SpringBootTest
public class IndexingServiceTest {
    @Autowired
    PageRepository pageRepository;
    @Autowired
    SiteRepository siteRepository;
    @Autowired
    IndexRepository indexRepository;
    @Autowired
    LemmaRepository lemmaRepository;
    @Test
    public void testIndexServiceTime(){ //15200
        long begin = System.currentTimeMillis();
        SitesList sl = new SitesList();
        Site s = new Site();
        s.setName("fitness house");
        s.setUrl("https://market.fitnesshouse.ru");
        sl.setSites(List.of(s));

        IndexingService indexingService = new IndexingService(sl);
        indexingService.startIndexing();

        Assert.notEmpty(pageRepository.findAll());
        Assert.notEmpty(siteRepository.findAll());
        Assert.notEmpty(indexRepository.findAll());
        Assert.notEmpty(lemmaRepository.findAll());
        Assertions.assertEquals(Status.INDEXED, siteRepository.findAll().get(0).getStatus());
        System.out.println(System.currentTimeMillis() - begin + " мс");
    }
}
