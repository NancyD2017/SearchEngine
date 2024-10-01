package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.model.*;

import java.io.IOException;
import java.util.*;

@RequiredArgsConstructor
public class PageGetterInfo {
    private final List<Lemma> lemmasList;
    private HashMap<Page, Double> relRelevances = new HashMap<>();

    public HashMap<Page, Double> findRelevanceRel() {
        lemmasList.forEach(l ->
                l.getIndexes()
                        .forEach((Index index) ->
                                relRelevances.compute(index.getPage(), (page, currentRel) ->
                                        currentRel == null
                                                ? index.getRank()
                                                : currentRel + index.getRank())));
        try {
            Double maxRel = relRelevances.values().stream().max(Comparator.naturalOrder()).get();
            relRelevances.replaceAll((page, absRel) -> absRel / maxRel);
        } catch (NoSuchElementException ignored) {
        }

        return relRelevances;
    }

    public String findPageTitle(Page page) {
        try {
            Document doc = Jsoup.connect(page.getPath()).get();
            return doc.title();
        } catch (IOException e) {
            return page.getSite().getName();
        }
    }
}
