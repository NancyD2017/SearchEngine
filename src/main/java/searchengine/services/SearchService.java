package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.*;
import searchengine.model.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static searchengine.controllers.ApiController.*;

@Service
@RequiredArgsConstructor
public class SearchService {
    private Map<String, Integer> sortedQuery;
    private List<SearchData> searchDataList;
    private List<Page> pageList;
    private List<Lemma> allLemmaList;

    public SearchResponse search(String query, String url, int offset, int limit) {
        SearchResponse response = new SearchResponse();
        createSortedQuery(query);
        int querySize = sortedQuery.size();

        List<Site> sites = siteRepository.findSiteByUrl(url) == null
                ? siteRepository.findAll()
                : List.of(siteRepository.findSiteByUrl(url));

        AtomicBoolean isFirst = new AtomicBoolean(true);
        List<SearchData> resultList = new ArrayList<>();
        sites.forEach(s -> resultList.addAll(proceedSearch(querySize, isFirst, offset, limit, response, s)));

        if (query.isEmpty() || query.isBlank()) {
            response.setError("Задан пустой поисковый запрос");
        } else {
            response.setResult(true);
            response.setData(resultList
                    .stream()
                    .sorted(Comparator.comparingDouble(SearchData::getRelevance).reversed())
                    .toList());
        }
        return response;
    }
    private void createSortedQuery(String query){
        Lemmatisation lemmatisation = new Lemmatisation(query.toLowerCase());
        try {
            Lemmatisation.luceneMorph = new RussianLuceneMorphology();
        } catch (IOException ignored) {
        }

        HashMap<String, Integer> processedQuery = lemmatisation.startLemmatisation();
        sortedQuery = processedQuery.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getValue))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> {
                            throw new AssertionError();
                        },
                        LinkedHashMap::new
                ));
    }
    private void createArrays(){
        searchDataList = new ArrayList<>();
        pageList = new ArrayList<>();
        allLemmaList = new ArrayList<>();
    }
    private List<SearchData> proceedSearch(int querySize, AtomicBoolean isFirst, int offset, int limit, SearchResponse response, Site s){
        createArrays();
        sortedQuery.forEach((word, count) -> {
            if (querySize <= 10 || count <= 3 * querySize) {

                List<Lemma> lemmasList = lemmaRepository.findLemmaBySiteIdAndLemma(s.getId(), word);
                allLemmaList.addAll(lemmasList);
                List<Page> currentPageList = new ArrayList<>();

                lemmasList.forEach(l -> l.getIndexes()
                        .forEach(i -> currentPageList.add(indexRepository.findIndexByIndexId(i.getId()).getPage())));

                if (isFirst.get()) {
                    isFirst.set(false);
                    pageList.addAll(currentPageList);
                } else pageList.retainAll(currentPageList);
            }
        });
        PageGetterInfo pgr = new PageGetterInfo(allLemmaList);
        HashMap<Page, Double> pageDoubleHashMap = pgr.findRelevanceRel();

        pageList.forEach(p -> {
            SnippetCreator snippetCreator = new SnippetCreator(p, allLemmaList, offset, limit);
            List<SearchData> searchData = snippetCreator.createSnippet(pgr, pageDoubleHashMap);
            searchDataList.addAll(searchData);
            response.setCount(response.getCount() + snippetCreator.countOfSnippets);
        });
        return searchDataList;
    }
}