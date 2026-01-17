package com.declutter.util;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.html2md.converter.LinkConversion;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarkdownUtil {
  private static final Pattern MARKDOWN_IMAGE = Pattern.compile("!\\[[^\\]]*\\]\\(([^\\s)]+)(?:\\s+\"[^\"]*\")?\\)");
  private static final Parser PARSER;
  private static final HtmlRenderer RENDERER;
  private static final FlexmarkHtmlConverter HTML_CONVERTER;

  static {
    MutableDataSet options = new MutableDataSet();
    options.set(Parser.EXTENSIONS, List.of(
        TablesExtension.create(),
        StrikethroughExtension.create(),
        AutolinkExtension.create()));
    options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

    // Configure HTML to Markdown converter to preserve links
    options.set(FlexmarkHtmlConverter.EXT_INLINE_LINK, LinkConversion.MARKDOWN_EXPLICIT);
    options.set(FlexmarkHtmlConverter.OUTPUT_ATTRIBUTES_ID, false);

    PARSER = Parser.builder(options).build();
    RENDERER = HtmlRenderer.builder(options).build();
    HTML_CONVERTER = FlexmarkHtmlConverter.builder(options).build();
  }

  private MarkdownUtil() {}

  public static String markdownToHtml(String markdown) {
    return RENDERER.render(PARSER.parse(markdown));
  }

  public static String htmlToMarkdown(String html, String hostname) {
    if (html == null || html.isBlank()) {
      return "";
    }
    Document doc = Jsoup.parse(html);
    doc.select("script, style, iframe, object, embed, noscript").remove();

    Map<String, String> replacements = new LinkedHashMap<>();
    AtomicInteger counter = new AtomicInteger(0);

    // Handle images - convert to markdown with absolute URLs
    for (Element img : doc.select("img")) {
      if (isLikelySiteLogo(img)) {
        img.remove();
        continue;
      }
      String alt = img.attr("alt");
      String src = img.attr("src");
      String title = img.attr("title");
      String normalized = normalizeUrl(src, hostname);
      String titlePart = title == null || title.isBlank() ? "" : " \"" + title + "\"";
      String markdown = normalized == null || normalized.isBlank()
          ? ""
          : "![" + alt + "](" + normalized + titlePart + ")";
      String placeholder = "@@IMG_" + counter.getAndIncrement() + "@@";
      replacements.put(placeholder, markdown);
      img.replaceWith(new TextNode(placeholder));
    }

    // Handle links - convert to markdown with absolute URLs
    for (Element link : doc.select("a[href]")) {
      String href = link.attr("href");
      String text = link.text().trim();
      String title = link.attr("title");

      // Skip empty links, anchor-only links, or javascript links
      if (href.isBlank() || href.startsWith("#") || href.startsWith("javascript:")) {
        link.unwrap(); // Just keep the text content
        continue;
      }

      // Skip links with no meaningful text
      if (text.isBlank()) {
        link.unwrap();
        continue;
      }

      String normalized = normalizeUrl(href, hostname);
      String titlePart = title == null || title.isBlank() ? "" : " \"" + title + "\"";
      String markdown = "[" + text + "](" + normalized + titlePart + ")";
      String placeholder = "@@LINK_" + counter.getAndIncrement() + "@@";
      replacements.put(placeholder, markdown);
      link.replaceWith(new TextNode(placeholder));
    }

    // Handle tables
    for (Element table : doc.select("table")) {
      String markdown = tableToMarkdown(table);
      String placeholder = "@@TABLE_" + counter.getAndIncrement() + "@@";
      replacements.put(placeholder, markdown);
      table.replaceWith(new TextNode(placeholder));
    }

    String converted = HTML_CONVERTER.convert(doc.html());
    for (Map.Entry<String, String> entry : replacements.entrySet()) {
      converted = converted.replace(entry.getKey(), entry.getValue());
    }
    return converted;
  }

  public static String removeNonImageMarkdownImages(String markdown) {
    if (markdown == null || markdown.isBlank()) {
      return markdown == null ? "" : markdown;
    }
    Matcher matcher = MARKDOWN_IMAGE.matcher(markdown);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      String url = matcher.group(1);
      if (isLikelyImageUrl(url)) {
        matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
      } else {
        matcher.appendReplacement(sb, "");
      }
    }
    matcher.appendTail(sb);
    String cleaned = sb.toString().replaceAll("\\n{3,}", "\n\n");
    return removeEmptyImagesSection(cleaned);
  }

  private static boolean isLikelySiteLogo(Element img) {
    String alt = img.attr("alt").toLowerCase();
    String title = img.attr("title").toLowerCase();
    String src = img.attr("src").toLowerCase();
    String className = img.className().toLowerCase();
    String id = img.id().toLowerCase();
    if (alt.contains("logo") || title.contains("logo")) {
      return true;
    }
    if (src.contains("logo") || src.contains("branding") || src.contains("masthead")) {
      return true;
    }
    if (className.contains("logo") || className.contains("brand") || className.contains("masthead")) {
      return true;
    }
    if (id.contains("logo") || id.contains("brand") || id.contains("masthead")) {
      return true;
    }
    for (Element parent : img.parents()) {
      String tag = parent.tagName();
      if ("header".equals(tag) || "footer".equals(tag) || "nav".equals(tag)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isLikelyImageUrl(String url) {
    if (url == null || url.isBlank()) {
      return false;
    }
    String lower = url.toLowerCase();
    int queryIndex = lower.indexOf('?');
    String path = queryIndex >= 0 ? lower.substring(0, queryIndex) : lower;
    if (path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg")
        || path.endsWith(".gif") || path.endsWith(".webp") || path.endsWith(".svg")
        || path.endsWith(".bmp") || path.endsWith(".tiff") || path.endsWith(".avif")) {
      return true;
    }
    String query = queryIndex >= 0 ? lower.substring(queryIndex + 1) : "";
    return query.contains("format=png") || query.contains("format=jpg")
        || query.contains("format=jpeg") || query.contains("format=gif")
        || query.contains("format=webp") || query.contains("format=svg")
        || query.contains("format=avif");
  }

  private static String removeEmptyImagesSection(String markdown) {
    String[] lines = markdown.split("\\R", -1);
    StringBuilder sb = new StringBuilder();
    int i = 0;
    while (i < lines.length) {
      String line = lines[i];
      if (line.matches("^#{1,6}\\s*Images\\s*$")) {
        int j = i + 1;
        while (j < lines.length && lines[j].isBlank()) {
          j++;
        }
        if (j >= lines.length || lines[j].matches("^#{1,6}\\s+.*$")) {
          i = j;
          continue;
        }
      }
      sb.append(line);
      if (i < lines.length - 1) {
        sb.append("\n");
      }
      i++;
    }
    return sb.toString().replaceAll("\\n{3,}", "\n\n");
  }

  private static String normalizeUrl(String url, String hostname) {
    if (url == null || url.isBlank()) {
      return url;
    }
    if (url.matches("^https?://.*")) {
      return url;
    }
    if (url.startsWith("//")) {
      return "https:" + url;
    }
    if (url.startsWith("/")) {
      return "https://" + hostname + url;
    }
    return "https://" + hostname + "/" + url;
  }

  private static String tableToMarkdown(Element table) {
    StringBuilder markdown = new StringBuilder();
    List<Element> rows = table.select("tr");
    for (int i = 0; i < rows.size(); i++) {
      Element row = rows.get(i);
      List<Element> cells = row.select("td, th");
      String rowContent = cells.stream()
          .map(cell -> cell.text().trim())
          .reduce((a, b) -> a + " | " + b)
          .orElse("");
      markdown.append("| ").append(rowContent).append(" |").append("\n");
      if (i == 0 && !cells.isEmpty()) {
        String separators = cells.stream().map(cell -> "---").reduce((a, b) -> a + " | " + b).orElse("");
        markdown.append("| ").append(separators).append(" |").append("\n");
      }
    }
    if (markdown.length() == 0) {
      return "";
    }
    return "\n" + markdown + "\n";
  }
}
