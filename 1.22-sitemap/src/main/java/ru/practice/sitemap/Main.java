package ru.practice.sitemap;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * Запуск: Main &lt;url&gt; [файл] [лимит страниц, 0 — без лимита] [число потоков]
 */
public class Main {

    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "https://sendel.ru/";
        Path output = Path.of(args.length > 1 ? args[1] : "sitemap.txt");
        int maxPages = args.length > 2 ? Integer.parseInt(args[2]) : 0;
        int parallelism = args.length > 3 ? Integer.parseInt(args[3]) : Runtime.getRuntime().availableProcessors();

        System.out.printf("Сайт: %s, потоков: %d, лимит страниц: %s%n",
                url, parallelism, maxPages > 0 ? maxPages : "нет");
        Instant start = Instant.now();

        Set<String> pages = new SiteCrawler(url, maxPages).crawl(url, parallelism);
        new SiteMapWriter().write(url, pages, output);

        System.out.printf("Готово: %d страниц за %d с, карта сохранена в %s%n",
                pages.size(), Duration.between(start, Instant.now()).toSeconds(), output.toAbsolutePath());
    }
}
