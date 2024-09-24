package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import searchengine.model.*;

import java.io.IOException;
import java.util.*;

@RequiredArgsConstructor
public class SnippetCreator {
    StringBuilder snippets = new StringBuilder();
    private final Page p;
    private final List<Lemma> queryWords;
    private LuceneMorphology luceneMorphology;
    private final int offset;
    private final int limit;
    private HashMap<String, List<String>> normalFormsOfContextWords = new HashMap<>();
    private List<String> queryW = new ArrayList<>();

    public String createSnippet() {
        String content = Jsoup.parse(p.getContent()).text().toLowerCase();
        lemmatisationForContent(content);
        queryW = queryWords.stream().map(Lemma::getLemma).toList();
        int currentOffset = 0;
        int currentLimit = 0;

        for (int word = 0; word < queryWords.size(); word++) {
            String alteredContent = content;

            List<String> formInContent = normalFormsOfContextWords.get(queryW.get(word));
            for (String s : formInContent) {
                int wordIndex = alteredContent.indexOf(s);
                while (wordIndex != -1) {
                    currentOffset++;
                    int snippetStart = Math.max(0, wordIndex - 120);
                    int snippetEnd = Math.min(alteredContent.length(), wordIndex + 120);
                    String snippetContent = alteredContent.substring(snippetStart, snippetEnd);
                    String readySnippet = beautifySnippet(snippetContent);

                    if (currentLimit < limit && currentOffset > offset) {
                        currentLimit++;
                        if (!snippetsContainWord(queryW, word, readySnippet)) {
                            snippets.append("<p>").append(readySnippet).append("</p>");
                        }
                    }
                    alteredContent = alteredContent.substring(snippetEnd);
                    wordIndex = alteredContent.indexOf(s, wordIndex);
                }
            }
        }
        return boldAllWords(snippets.toString());
    }

    private void lemmatisationForContent(String content) {
        try {
            luceneMorphology = new RussianLuceneMorphology();
        } catch (IOException ignored) {
        }
        String[] contentSplit = content.split("[^А-я]+");
        for (String word : contentSplit) {
            String normalizedWord = luceneMorphology.getNormalForms(word).get(0);
            if (!normalFormsOfContextWords.containsKey(normalizedWord)) {
                normalFormsOfContextWords.put(normalizedWord, new ArrayList<>(List.of(word)));
            } else {
                normalFormsOfContextWords.get(normalizedWord).add(word);
            }
        }
    }

    private String boldAllWords(String snippets) {
        for (String word : queryW) {
            for (String formedWord : normalFormsOfContextWords.get(word)) {
                int firstIndex = snippets.indexOf(formedWord);

                while (firstIndex != -1) {
                    int wordEnd = snippets.substring(firstIndex).indexOf(' ');
                    boolean wordIsMarked = snippets.charAt(firstIndex - 1) == '>' && snippets.charAt(firstIndex + wordEnd - 1) == '>';
                    if (!wordIsMarked) {
                        snippets = snippets.substring(0, firstIndex)
                                + "<b>"
                                + snippets.substring(firstIndex, firstIndex + wordEnd)
                                + "</b>"
                                + snippets.substring(firstIndex + wordEnd);
                    }

                    firstIndex = snippets.indexOf(formedWord, firstIndex + wordEnd);
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
            if (snippet.contains(queryWords.get(i))) return true;
        }
        return false;
    }

    private String beautifySnippet(String trimmedContent) {
        int beginIndex = trimmedContent.indexOf(' ');
        int endIndex = trimmedContent.lastIndexOf(' ');

        return trimmedContent.substring(beginIndex, endIndex);
    }
}
