package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import searchengine.dto.indexing.SearchData;
import searchengine.model.*;

import java.io.IOException;
import java.util.*;

public class SnippetCreator {
    private final Page p;
    private final List<Lemma> queryWords;
    private LuceneMorphology luceneMorphology;
    private final int offset;
    private final int limit;
    private HashMap<String, List<String>> normalFormsOfContextWords = new HashMap<>();
    private List<String> queryW;
    public int countOfSnippets = 0;
    private final String content;

    public SnippetCreator(Page p, List<Lemma> queryWords, int offset, int limit) {
        this.p = p;
        this.queryWords = queryWords;
        this.offset = offset;
        this.limit = limit;
        try {
            this.luceneMorphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.queryW = queryWords.stream().map(Lemma::getLemma).toList();

        this.content = Jsoup.parse(p.getContent()).text().toLowerCase();
        String[] contentSplit = content.split("[^А-я]+");
        for (String word : contentSplit) {
            if (!word.isBlank() && !word.isEmpty()) {
                String normalizedWord = luceneMorphology.getNormalForms(word).get(0);
                normalFormsOfContextWords.computeIfAbsent(normalizedWord, k -> new ArrayList<>()).add(word);
            }
        }
    }

    public List<SearchData> createSnippet(PageGetterInfo pgr, HashMap<Page, Double> pageDoubleHashMap) {
        List<String> snippets = new ArrayList<>();
        int currentOffset = 0;
        int currentLimit = 0;

        for (int word = 0; word < queryWords.size(); word++) {
            String alteredContent = this.content;

            for (String s : normalFormsOfContextWords.get(queryW.get(word))) {
                int wordIndex = alteredContent.indexOf(" " + s);
                while (wordIndex != -1) {
                    currentOffset++;
                    int snippetStart = Math.max(0, wordIndex - 120);
                    int snippetEnd = Math.min(alteredContent.length(), wordIndex + 120);
                    String readySnippet = beautifySnippet(alteredContent.substring(snippetStart, snippetEnd), s);

                    if (currentLimit < limit && currentOffset > offset) {
                        currentLimit++;
                        if (!snippetsContainWord(queryW, word, readySnippet)) {
                            countOfSnippets++;
                            snippets.add(boldAllWords(readySnippet));
                        }
                    }
                    alteredContent = alteredContent.substring(snippetEnd);
                    wordIndex = alteredContent.indexOf(" " + s, wordIndex);
                }
            }
        }
        return makeData(snippets, pgr, pageDoubleHashMap);
    }

    private List<SearchData> makeData(List<String> snippets, PageGetterInfo pgr, HashMap<Page, Double> pageDoubleHashMap) {
        List<SearchData> searchDataList = new ArrayList<>();

        snippets.forEach(s -> {
            SearchData searchData = new SearchData();
            searchData.setSiteName(p.getSite().getName());
            searchData.setSite(p.getSite().getUrl());
            searchData.setTitle(pgr.findPageTitle(p));
            searchData.setUri(p.getPath()
                    .contains(p.getSite().getUrl()) ? p.getPath().replace(p.getSite().getUrl(), "") : "");
            searchData.setRelevance(pageDoubleHashMap.get(p));
            searchData.setSnippet(s);
            searchDataList.add(searchData);
        });

        return searchDataList;
    }

    private String boldAllWords(String snippets) {
        for (String word : queryW.stream().sorted(Comparator.comparing(String::length).reversed()).toList()) {
            for (String formedWord : normalFormsOfContextWords.get(word).stream().sorted(Comparator.comparing(String::length).reversed()).toList()) {
                if (!snippets.contains("<b>" + formedWord + "</b>")) {
                    snippets = snippets.replaceAll(" " + formedWord, " <b>" + formedWord + "</b>");
                }
            }
        }
        return snippets;
    }

    private boolean snippetsContainWord(List<String> queryWords, int currentWord, String snippet) {
        StringBuilder sb = new StringBuilder();
        String[] contentSplit = Arrays.stream(snippet.split("[^А-я]+"))
                .filter(c -> !c.isBlank() && !c.isEmpty())
                .toArray(String[]::new);

        for (String word : contentSplit) {
            sb.append(luceneMorphology.getNormalForms(word).get(0)).append(" ");
        }

        snippet = sb.toString();
        for (int i = 0; i < currentWord; i++) {
            if (snippet.contains(" " + queryWords.get(i))) return true;
        }
        return false;
    }

    private String beautifySnippet(String trimmedContent, String s) {
        int beginIndex = trimmedContent.indexOf(' ');
        int endIndex = trimmedContent.lastIndexOf(' ');

        if (!trimmedContent.substring(beginIndex, endIndex).contains(s)) {
            if (!trimmedContent.substring(beginIndex).contains(s)) return trimmedContent.substring(0, endIndex);
            else if (!trimmedContent.substring(0, endIndex).contains(s)) return trimmedContent.substring(beginIndex);
            else return trimmedContent;
        }
        return trimmedContent.substring(beginIndex, endIndex);
    }
}
