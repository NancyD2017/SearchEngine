package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Service;
import searchengine.config.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.*;
import searchengine.model.Site;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static searchengine.controllers.ApiController.*;

@Service
@RequiredArgsConstructor
public class IndexingService {
    private final SitesList sites;
    private AtomicBoolean isIndexingStopped = new AtomicBoolean(false);
    private final ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
    private final HashMap<String, Lemma> lemmasNamesAndLemmas = new HashMap<>();
    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    IndexingResponse fillIndexLemmaDatabasesError = null;
    private final ConcurrentHashMap<String, Page> cache = new ConcurrentHashMap<>();

    public IndexingResponse startIndexing() {
        IndexingResponse response = new IndexingResponse();
        List<Site> sitesList = siteRepository.findAll();
        if (!sitesList.isEmpty() && sitesList.get(sitesList.size() - 1).getStatus() == Status.INDEXING) {
            response.setResult(false);
            response.setError("Индексация уже запущена");
            return response;
        }
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<?>> futures = new ArrayList<>();
        for (searchengine.config.Site site : sites.getSites()) {
            futures.add(createIndexingTask(site, executor));
        }
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                response.setResult(false);
                response.setError("Ошибка многопоточности");
                return response;
            }
        }
        executor.shutdown();
        response.setResult(true);
        return response;
    }

    public IndexingResponse stopIndexing() {
        isIndexingStopped = new AtomicBoolean(true);
        IndexingResponse response = new IndexingResponse();
        response.setResult(true);
        response.setError(null);
        if (siteRepository.findAll().get(siteRepository.findAll().size() - 1).getStatus() != Status.INDEXING) {
            response.setError("Индексация не запущена");
        }
        return response;
    }

    public IndexingResponse indexPage(String path) {
        IndexingResponse r = assignLuceneMorph();
        if (r.isResult()) return r;
        IndexingResponse response = new IndexingResponse();

        if (pageRepository.findAll().stream().map(Page::getPath).toList().contains(path)) {
            response.setResult(false);
            response.setError("Данная страница уже была проиндексирована");
            return response;
        }
        boolean toBreak = proceedIndexingPage(path);

        if (!toBreak) {
            response.setResult(false);
            response.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
            return response;
        }

        response.setResult(true);
        return response;
    }
    private Future<?> createIndexingTask(searchengine.config.Site site, ExecutorService executor){
        Runnable task = () -> {
            findEntityInSiteRepository(site.getName(), site.getUrl())
                    .ifPresent(sitesL -> {
                        List<Page> pages = pageRepository.findPagesBySite(sitesL);
                        List<Index> indexes = new ArrayList<>();
                        List<Lemma> lemmas = lemmaRepository.findLemmaBySiteId(sitesL.getId());
                        pages.forEach(p -> indexes.addAll(indexRepository.findIndexesByPageId(p.getId())));
                        indexRepository.deleteAll(indexes);
                        lemmaRepository.deleteAll(lemmas);
                        pageRepository.deleteAll(pages);
                        siteRepository.delete(sitesL);
                    });

            Site siteEntity = new Site();
            siteEntity.setName(site.getName());
            siteEntity.setUrl(site.getUrl());
            siteEntity.setLastError("");
            siteEntity.setStatusTime(LocalDateTime.now());
            siteEntity.setStatus(Status.INDEXING);
            siteRepository.save(siteEntity);

            indexingForPageDatabase(site, siteEntity);
            if (fillIndexLemmaDatabasesError != null) {
                throw new RuntimeException(String.valueOf(fillIndexLemmaDatabasesError));
            }
        };
        Future<?> future = executor.submit(task);
        return future;
    }

    private boolean proceedIndexingPage(String path) {
        List<Site> sites = siteRepository.findAll();
        boolean toBreak = false;
        for (Site site : sites) {
            List<String> paths = pageRepository.findPagesBySite(site).stream().map(Page::getPath).toList();
            Set<String> linksSet = new TreeSet<>(pool.invoke(new SiteGetterLinks(site.getUrl())));
            paths.forEach(linksSet::remove);

            for (String l : linksSet) {
                if (l.equals(path)) {
                    toBreak = true;
                    processLinks(site, new TreeSet<>(Collections.singleton(l)));
                    break;
                }
            }

            if (toBreak) break;
        }
        return toBreak;
    }

    private void fillIndexLemmaDatabases(Page page, Site siteEntity) {
        assignLuceneMorph();
        Lemmatisation lemmatisation = new Lemmatisation(page.getContent());
        proceedLemmatisation(lemmatisation, page, siteEntity);
    }

    private IndexingResponse assignLuceneMorph() {
        IndexingResponse response = new IndexingResponse();
        try {
            Lemmatisation.luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            response.setResult(false);
            response.setError("Не удалось подключить библиотеку RussianLuceneMorphology");
            fillIndexLemmaDatabasesError = response;
        }
        return response;
    }

    private void proceedLemmatisation(Lemmatisation lemmatisation, Page page, Site siteEntity) {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<List<Index>>> futures = new ArrayList<>();
        HashMap<String, Integer> lemmas = lemmatisation.startLemmatisation();

        int chunkSize = lemmas.size() / Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
            int start = i * chunkSize;
            int end = (i == Runtime.getRuntime().availableProcessors() - 1) ? lemmas.size() : start + chunkSize;
            Map<String, Integer> wordCounts = lemmas.entrySet().stream()
                    .skip(start)
                    .limit(end - start)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            futures.add(createLemmatisationTask(executor, wordCounts, siteEntity, page));
        }

        List<Index> indexList = new ArrayList<>();
        for (Future<List<Index>> future : futures) {
            try {
                indexList.addAll(future.get());
            } catch (Exception ignored) {
            }
        }
        executor.shutdown();
        indexRepository.saveAll(indexList);
    }
    private Future<List<Index>> createLemmatisationTask(ExecutorService executor, Map<String, Integer> wordCounts, Site siteEntity, Page page){
        return executor.submit(() -> {
            List<Index> indexList = new ArrayList<>();
            wordCounts.forEach((word, count) -> {
                Lemma currentLemma = lemmasNamesAndLemmas.get(word);
                if (currentLemma == null || (!currentLemma.getLemma().equals(word) || currentLemma.getSite() != siteEntity)) {
                    Lemma ld = new Lemma();
                    ld.setLemma(word);
                    ld.setSite(siteEntity);
                    indexList.add(saveLemmaIndex(ld, 1, word, page.getSite(), page, count));
                    lemmasNamesAndLemmas.put(word, ld);
                } else {
                    indexList.add(
                            saveLemmaIndex(currentLemma,
                                    currentLemma.getFrequency() + 1,
                                    currentLemma.getLemma(),
                                    currentLemma.getSite(),
                                    page,
                                    count));
                }
            });
            return indexList;
        });
    }

    private Index saveLemmaIndex(Lemma lemma, int frequency, String lemmaWord, Site site, Page page, int rank) {
        lemma.setFrequency(frequency);
        lemma.setLemma(lemmaWord);
        lemma.setSite(site);
        lemmasNamesAndLemmas.put(lemmaWord, lemma);
        lemmaRepository.save(lemma);

        Index index = new Index();
        index.setPage(page);
        index.setLemma(lemma);
        index.setRank(rank);
        return index;
    }

    private Optional<Site> findEntityInSiteRepository(String siteName, String url) {
        if (siteRepository != null) {
            Site siteDatabases = siteRepository.findSiteByNameAndUrl(siteName, url);
            return siteDatabases == null ? Optional.empty() : Optional.of(siteDatabases);
        }
        return Optional.empty();
    }

    private void indexingForPageDatabase(searchengine.config.Site site, Site siteEntity) {
        if (!isIndexingStopped.get()) {
            TreeSet<String> linksSet = new TreeSet<>(pool.invoke(new SiteGetterLinks(site.getUrl())));
            processLinks(siteEntity, linksSet);
        }

        updateSiteStatus(siteEntity);
        siteRepository.save(siteEntity);
    }

    private void processLinks(Site siteEntity, TreeSet<String> linksSet) {
        linksSet.forEach(l -> processLink(l, siteEntity));
    }

    private void processLink(String link, Site siteEntity) {
        if (!isIndexingStopped.get()) {
            Page pageEntity = new Page();
            try {
                Page page = getValues(siteEntity, pageEntity, link);
                pageRepository.save(page);
                if (page.getCode() < 400) {
                    fillIndexLemmaDatabases(page, siteEntity);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void updateSiteStatus(Site siteEntity) {
        if (!isIndexingStopped.get()) {
            siteEntity.setStatus(Status.INDEXED);
        } else {
            siteEntity.setStatus(Status.FAILED);
            siteEntity.setLastError("Индексация остановлена пользователем");
            isIndexingStopped = new AtomicBoolean(false);
        }
    }

    private Page getValues(Site siteEntity, Page pageEntity, String l) {
        if (cache.containsKey(l)) {
            return cache.get(l);
        }
        Page page = getValuesImpl(siteEntity, pageEntity, l);
        cache.put(l, page);
        return page;
    }

    private Page getValuesImpl(Site siteEntity, Page pageEntity, String l) {
        try {
            HttpGet request = new HttpGet(l);
            HttpResponse response = httpClient.execute(request);
            pageEntity.setCode(response.getStatusLine().getStatusCode());

            pageEntity.setSite(siteEntity);
            pageEntity.setPath(l);
            if (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK) {
                String content = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                pageEntity.setContent(content);
            } else {
                pageEntity.setContent("");
                siteEntity.setStatus(Status.FAILED);
                siteEntity.setLastError("Не удалось установить соединение с одной из страниц");
                siteRepository.save(siteEntity);
            }
            siteEntity.setStatusTime(LocalDateTime.now());
        } catch (Exception e) {
            siteEntity.setStatus(Status.FAILED);
            siteEntity.setLastError(e.getMessage());
            siteRepository.save(siteEntity);
        }
        return pageEntity;
    }
}
