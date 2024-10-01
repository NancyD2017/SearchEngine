package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.SearchData;
import searchengine.dto.indexing.SearchResponse;
import searchengine.model.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static searchengine.controllers.ApiController.*;

@Service
@RequiredArgsConstructor
public class SearchService {
    public SearchResponse search(String query, String url, int offset, int limit) {
        SearchResponse response = new SearchResponse();

        Lemmatisation lemmatisation = new Lemmatisation(query.toLowerCase());
        try {
            Lemmatisation.luceneMorph = new RussianLuceneMorphology();
        } catch (IOException ignored) {
        }

        HashMap<String, Integer> processedQuery = lemmatisation.startLemmatisation();
        Map<String, Integer> sortedQuery = processedQuery.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getValue))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> {
                            throw new AssertionError();
                        },
                        LinkedHashMap::new
                ));

        int querySize = sortedQuery.size();

        List<Site> sites =
                siteRepository.findSiteByUrl(url) == null
                ? siteRepository.findAll()
                : List.of(siteRepository.findSiteByUrl(url));

        List<SearchData> searchDataList = new ArrayList<>();

        List<Page> pageList = new ArrayList<>();
        List<Lemma> allLemmaList = new ArrayList<>();
        AtomicBoolean isFirst = new AtomicBoolean(true);
        sites.forEach(s -> {
            sortedQuery.forEach((word, count) -> {
                if (querySize <= 10 || count <= 3 * querySize) {

                    List<Lemma> lemmasList = lemmaRepository.findLemmaBySiteIdAndLemma(s.getId(), word);
                    allLemmaList.addAll(lemmasList);
                    List<Page> currentPageList = new ArrayList<>();

                    lemmasList.forEach(l ->
                            l.getIndexes()
                                    .forEach(i ->
                                            currentPageList.add(
                                                    indexRepository.findIndexByIndexId(i.getId()).getPage()
                                            )
                                    )
                    );

                    if (isFirst.get()) {
                        isFirst.set(false);
                        pageList.addAll(currentPageList);
                    } else pageList.retainAll(currentPageList);
                }
            });
            PageGetterInfo pgr = new PageGetterInfo(allLemmaList);
            HashMap<Page, Double> pageDoubleHashMap = pgr.findRelevanceRel();

            long begin = System.currentTimeMillis();
            pageList.forEach(p -> {
                SnippetCreator snippetCreator = new SnippetCreator(p, allLemmaList, offset, limit);
                List<SearchData> searchData = snippetCreator.createSnippet(pgr, pageDoubleHashMap);
                searchDataList.addAll(searchData);
                response.setCount(response.getCount() + snippetCreator.countOfSnippets);
            });
            System.out.println("Закончил обработку page " + (System.currentTimeMillis() - begin));

        });

        if (query.isEmpty() || query.isBlank()) {
            response.setError("Задан пустой поисковый запрос");
        } else {
            response.setResult(true);
            response.setData(searchDataList
                    .stream()
                    .sorted(Comparator.comparingDouble(SearchData::getRelevance))
                    .toList());
        }
        return response;
    }
}
