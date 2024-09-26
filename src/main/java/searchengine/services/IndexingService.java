package searchengine.services;

import lombok.RequiredArgsConstructor;
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
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

import static searchengine.controllers.ApiController.*;

@Service
@RequiredArgsConstructor
public class IndexingService {
    private final SitesList sites;
    private AtomicBoolean isIndexingStopped = new AtomicBoolean(false);
    private final ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
    private final HashMap<String, Lemma> lemmasNamesAndLemmas = new HashMap<>();
    static ArrayList<Thread> lemmatisationThreads = new ArrayList<>();
    IndexingResponse fillIndexLemmaDatabasesError = null;

    public IndexingResponse startIndexing() {
        IndexingResponse response = new IndexingResponse();
        List<Site> sitesList = siteRepository.findAll();
        if (!sitesList.isEmpty() && sitesList.get(sitesList.size() - 1).getStatus() == Status.INDEXING) {
            response.setResult(false);
            response.setError("Индексация уже запущена");
            return response;
        }
        for (searchengine.config.Site site : sites.getSites()) {

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
            if (fillIndexLemmaDatabasesError != null) return fillIndexLemmaDatabasesError;
        }
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
        try {
            Lemmatisation.luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            IndexingResponse response = new IndexingResponse();
            response.setResult(false);
            response.setError("Не удалось подключить библиотеку RussianLuceneMorphology");
            return response;
        }
        IndexingResponse response = new IndexingResponse();
        Site currentSiteEntity = siteRepository.findSiteByUrl(path);

        if (currentSiteEntity == null) {
            response.setResult(false);
            response.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
            return response;
        }

        List<String> paths = pageRepository.findPagesBySite(currentSiteEntity).stream().map(Page::getPath).toList();
        TreeSet<String> linksSet = new TreeSet<>(pool.invoke(new SiteGetterLinks(currentSiteEntity.getUrl())));
        linksSet.removeAll(paths);
        processLinks(currentSiteEntity, linksSet);

        response.setResult(true);

        return response;
    }

    private void fillIndexLemmaDatabases(Page page) {
        try {
            Lemmatisation.luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            IndexingResponse response = new IndexingResponse();
            response.setResult(false);
            response.setError("Не удалось подключить библиотеку RussianLuceneMorphology");
            fillIndexLemmaDatabasesError = response;
        }
        Lemmatisation lemmatisation = new Lemmatisation(page.getContent());
        proceedLemmatisation(lemmatisation, page);
    }

    private void proceedLemmatisation(Lemmatisation lemmatisation, Page page) {
        List<Index> indexList = Collections.synchronizedList(new ArrayList<>());
        lemmatisation.startLemmatisation()
                .forEach((word, count) -> {
                    Runnable createLemma = () -> {
                        Lemma currentLemma = lemmasNamesAndLemmas.get(word);
                        if (currentLemma == null) {
                            Lemma ld = new Lemma();
                            indexList.add(saveLemmaIndex(ld, 1, word, page.getSite(), page, count));
                        } else {
                            indexList.add(
                                    saveLemmaIndex(currentLemma,
                                            currentLemma.getFrequency() + 1,
                                            currentLemma.getLemma(),
                                            currentLemma.getSite(),
                                            page,
                                            count));
                        }
                    };
                    Thread t = new Thread(createLemma);
                    lemmatisationThreads.add(t);
                    t.start();
                });
        runThreads(lemmatisationThreads);
        indexRepository.saveAll(indexList);
    }

    private static void runThreads(ArrayList<Thread> threads) {
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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
                        fillIndexLemmaDatabases(page);
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
        try {
            URL url = new URL(l);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();
            int code = connection.getResponseCode();

            pageEntity.setCode(code);
            pageEntity.setSite(siteEntity);
            pageEntity.setPath(l);
            if (code == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = connection.getInputStream()) {
                    String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    pageEntity.setContent(content);
                }
            } else {
                pageEntity.setContent("Error");
                siteEntity.setStatus(Status.FAILED);
                siteEntity.setLastError("Не удалось установить соединение с сайтом");
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
