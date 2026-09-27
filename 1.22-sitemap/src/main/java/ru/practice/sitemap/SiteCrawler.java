package ru.practice.sitemap;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Обходит сайт в ForkJoinPool: каждая страница — отдельная задача,
 * которая порождает подзадачи для найденных на ней ссылок.
 */
public class SiteCrawler {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/128.0 Safari/537.36";
    private static final int TIMEOUT_MS = 10_000;

    private final LinkFilter filter;
    private final int maxPages;
    /** Уже взятые в работу страницы — защита от циклического обхода графа. */
    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    /** Страницы, которые удалось загрузить. */
    private final Set<String> pages = ConcurrentHashMap.newKeySet();
    private final AtomicInteger counter = new AtomicInteger();

    public SiteCrawler(String rootUrl, int maxPages) {
        this.filter = new LinkFilter(rootUrl);
        this.maxPages = maxPages;
    }

    public Set<String> crawl(String rootUrl, int parallelism) {
        String root = filter.accept(rootUrl).orElseThrow(() -> new IllegalArgumentException("Некорректный адрес: " + rootUrl));
        visited.add(LinkFilter.key(root));
        ForkJoinPool pool = new ForkJoinPool(parallelism);
        try {
            pool.invoke(new PageTask(root));
        } finally {
            pool.shutdown();
        }
        return pages;
    }

    private boolean limitReached() {
        return maxPages > 0 && counter.get() >= maxPages;
    }

    private class PageTask extends RecursiveAction {

        private final String url;

        PageTask(String url) {
            this.url = url;
        }

        @Override
        protected void compute() {
            if (limitReached()) {
                return;
            }
            Document document = load();
            if (document == null) {
                return;
            }
            pages.add(url);
            System.out.printf("[%d] %s%n", counter.incrementAndGet(), url);

            List<PageTask> subtasks = new ArrayList<>();
            for (Element link : document.select("a[href]")) {
                if (limitReached()) {
                    break;
                }
                filter.accept(link.absUrl("href"))
                        .filter(child -> visited.add(LinkFilter.key(child)))
                        .ifPresent(child -> subtasks.add(new PageTask(child)));
            }
            invokeAll(subtasks);
        }

        private Document load() {
            try {
                // Пауза между запросами, чтобы сайт не заблокировал доступ
                Thread.sleep(ThreadLocalRandom.current().nextInt(100, 151));
                Connection.Response response = Jsoup.connect(url)
                        .userAgent(USER_AGENT)
                        .referrer("https://www.google.com")
                        .timeout(TIMEOUT_MS)
                        .followRedirects(true)
                        .execute();
                String contentType = response.contentType();
                if (contentType == null || !contentType.startsWith("text/html")) {
                    return null;
                }
                // После редиректа страница могла оказаться на другом домене
                if (filter.accept(response.url().toString()).isEmpty()) {
                    return null;
                }
                return response.parse();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                System.err.println("Не удалось загрузить " + url + ": " + e.getMessage());
                return null;
            }
        }
    }
}
