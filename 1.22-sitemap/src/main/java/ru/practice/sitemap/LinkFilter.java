package ru.practice.sitemap;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Приводит ссылки к единому виду и отсекает лишние:
 * чужие домены и поддомены, якоря (#), файлы, не-http ссылки.
 */
public class LinkFilter {

    private static final Set<String> SKIP_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "svg", "ico", "bmp",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "zip", "rar", "7z", "gz",
            "mp3", "mp4", "avi", "mov", "webm", "css", "js", "json", "xml", "rss", "txt");

    private final String host;

    public LinkFilter(String rootUrl) {
        this.host = stripWww(URI.create(rootUrl).getHost().toLowerCase(Locale.ROOT));
    }

    /** Возвращает нормализованную ссылку, если она подходит для карты сайта. */
    public Optional<String> accept(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return Optional.empty();
        }
        URI uri;
        try {
            uri = new URI(rawUrl.trim());
        } catch (Exception e) {
            return Optional.empty();
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            return Optional.empty();
        }
        String linkHost = uri.getHost();
        if (linkHost == null || !stripWww(linkHost.toLowerCase(Locale.ROOT)).equals(host)) {
            return Optional.empty();
        }
        String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
        if (isFile(path)) {
            return Optional.empty();
        }
        // Якорь (#...) указывает на элемент той же страницы, поэтому отбрасываем его
        StringBuilder url = new StringBuilder()
                .append(scheme.toLowerCase(Locale.ROOT)).append("://")
                .append(linkHost.toLowerCase(Locale.ROOT));
        if (uri.getPort() != -1) {
            url.append(':').append(uri.getPort());
        }
        url.append(path);
        if (uri.getRawQuery() != null) {
            url.append('?').append(uri.getRawQuery());
        }
        return Optional.of(url.toString());
    }

    /** Ключ для проверки «уже посещали»: без схемы, www и завершающего слеша. */
    public static String key(String url) {
        URI uri = URI.create(url);
        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        return stripWww(uri.getHost().toLowerCase(Locale.ROOT)) + path + query;
    }

    private static boolean isFile(String path) {
        int slash = path.lastIndexOf('/');
        int dot = path.lastIndexOf('.');
        if (dot <= slash) {
            return false;
        }
        return SKIP_EXTENSIONS.contains(path.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private static String stripWww(String host) {
        return host.startsWith("www.") ? host.substring(4) : host;
    }
}
