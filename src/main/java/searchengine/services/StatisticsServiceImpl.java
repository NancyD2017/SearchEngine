package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.*;
import searchengine.dto.statistics.*;
import searchengine.model.Site;

import java.util.ArrayList;
import java.util.*;
import static searchengine.controllers.ApiController.*;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SitesList sites;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(siteRepository.findAll().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = siteRepository.findAll();
        for (Site site : sitesList) {

            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());

            Site sd = siteRepository.findSiteByNameAndUrl(site.getName(), site.getUrl());
            int pages = sd == null ? 0 : pageRepository.findPagesBySite(sd).size();
            int lemmas = sd == null ? 0 : lemmaRepository.findLemmaBySiteId(sd.getId()).size();

            item.setPages(pages);
            item.setLemmas(lemmas);
            if (sd != null) {
                item.setStatus(sd.getStatus().toString());
                item.setError(sd.getLastError());
                item.setStatusTime(sd.getStatusTime());
            }

            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);

        }

        StatisticsResponse response = new StatisticsResponse();

        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);

        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
