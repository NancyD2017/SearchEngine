package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;

import java.util.*;

public class Lemmatisation {
    private final String text;
    public static LuceneMorphology luceneMorph;

    public Lemmatisation(String text) {
        this.text = clearFromTags(text);
    }
    public HashMap<String, Integer> startLemmatisation() {
        HashMap<String, Integer> lemmas = new HashMap<>();
        List<String> words = Arrays.stream(text.split("[^а-я]")).filter(i -> !i.isEmpty() && !i.isBlank()).toList();
        for (String word : words) {
            if (notConjunctionOrPreposition(word)) {
                String normalWord = luceneMorph.getNormalForms(word).get(0);
                lemmas.put(normalWord, lemmas.getOrDefault(normalWord, 0) + 1);
            }
        }
        return lemmas;
    }
    private boolean notConjunctionOrPreposition(String word){
        List<String> wordBaseForms = luceneMorph.getMorphInfo(word);
        for (String wbf: wordBaseForms){
            if (wbf.contains("МЕЖД") || wbf.contains("ПРЕДЛ") || wbf.contains("СОЮЗ") || wbf.contains("k")) return false;
        }
    return true;
    }

    private String clearFromTags(String text){
       return Jsoup.parse(text).text();
    }
}
