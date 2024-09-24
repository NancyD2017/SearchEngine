package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class SiteGetterLinks extends RecursiveTask<Set<String>> {
    private final String link;
    private static final Set<String> linkSet = ConcurrentHashMap.newKeySet();
    public SiteGetterLinks(String link){this.link = link;}

    @Override
    protected Set<String> compute(){
        Set<String> links = new TreeSet<>();
        List<SiteGetterLinks> taskList = new ArrayList<>();
        try {
            Thread.sleep(500);
            Document doc = Jsoup.connect(link)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .ignoreContentType(true).get();
            Elements element = doc.select("a");
            for (Element e: element){
                String absLink = e.absUrl("href");
                if (absLink.isEmpty() || !absLink.startsWith("http") || linkSet.contains(absLink)) {
                    continue;
                }
                if (absLink.endsWith("/")) absLink = absLink.substring(0, absLink.length() - 1);

                if (absLink.contains(".pdf") || absLink.contains(".png") || absLink.contains(".jpg") || absLink.contains("#") || absLink.contains(";") || absLink.contains(".com") || absLink.contains(".net")) {
                    continue;
                }

                linkSet.add(absLink);
                links.add(absLink);

            }
        } catch (IOException | InterruptedException | IllegalArgumentException e){
            System.out.println("ошибка " + e.getMessage());
        }
        for (SiteGetterLinks task: taskList){
            links.addAll(task.join());
        }
        return links;
    }
}
