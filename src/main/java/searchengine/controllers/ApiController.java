package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;
    public static SiteRepository siteRepository;
    public static PageRepository pageRepository;
    public static LemmaRepository lemmaRepository;
    public static IndexRepository indexRepository;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SearchService searchService, SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
        ApiController.siteRepository = siteRepository;
        ApiController.pageRepository = pageRepository;
        ApiController.lemmaRepository = lemmaRepository;
        ApiController.indexRepository = indexRepository;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestParam String url) {
        return ResponseEntity.ok(indexingService.indexPage(url));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam String query,
                                                 @RequestParam(required = false, defaultValue = "") String site,
                                                 @RequestParam(required = false, defaultValue = "0") int offset,
                                                 @RequestParam(required = false, defaultValue = "20") int limit)
    {
        return ResponseEntity.ok(searchService.search(query, site, offset, limit));
    }
}
