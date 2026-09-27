package ru.practice.sitemap;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Строит дерево страниц по вложенности путей и пишет его в файл:
 * каждая дочерняя страница сдвинута на одну табуляцию относительно родительской.
 */
public class SiteMapWriter {

    private record Node(String url, List<Node> children) {
        Node(String url) {
            this(url, new ArrayList<>());
        }
    }

    public void write(String rootUrl, Collection<String> pages, Path file) throws IOException {
        Node root = buildTree(rootUrl, pages);
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writeNode(writer, root, 0);
        }
    }

    private Node buildTree(String rootUrl, Collection<String> pages) {
        Map<String, Node> nodes = new HashMap<>();
        Node root = new Node(rootUrl);
        String rootKey = LinkFilter.key(rootUrl);
        nodes.put(rootKey, root);
        for (String page : pages) {
            nodes.putIfAbsent(LinkFilter.key(page), new Node(page));
        }
        for (Map.Entry<String, Node> entry : nodes.entrySet()) {
            if (entry.getKey().equals(rootKey)) {
                continue;
            }
            findParent(entry.getKey(), nodes, root).children().add(entry.getValue());
        }
        return root;
    }

    /** Родитель — ближайшая найденная страница, путь которой является префиксом пути ребёнка. */
    private Node findParent(String key, Map<String, Node> nodes, Node root) {
        String current = key.contains("?") ? key.substring(0, key.indexOf('?')) : parentOf(key);
        while (current != null) {
            Node parent = nodes.get(current);
            if (parent != null && !current.equals(key)) {
                return parent;
            }
            current = parentOf(current);
        }
        return root;
    }

    private String parentOf(String key) {
        int slash = key.lastIndexOf('/');
        return slash < 0 ? null : key.substring(0, slash);
    }

    private void writeNode(BufferedWriter writer, Node node, int depth) throws IOException {
        writer.write("\t".repeat(depth));
        writer.write(node.url());
        writer.newLine();
        node.children().sort(Comparator.comparing(n -> URI.create(n.url()).getRawPath() + n.url()));
        for (Node child : node.children()) {
            writeNode(writer, child, depth + 1);
        }
    }
}
