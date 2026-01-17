package com.declutter.output;

import com.declutter.scrape.Scraper;
import com.declutter.util.MarkdownUtil;
import com.declutter.util.ResourceLoader;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class OutputWriter {
  private static final String DECLUTTERED_DIRECTORY = "Decluttered";

  private final TemplateRenderer renderer;

  public OutputWriter(TemplateRenderer renderer) {
    this.renderer = renderer;
  }

  public void writeOutput(
      Scraper scraper,
      URL url,
      String markdown,
      String metadata,
      OutputFormat outputFormat,
      String styleName,
      Path outputDirectory) {
    PathParts parts = pathFromUrl(url);
    Path finalDir = outputDirectory
        .resolve(DECLUTTERED_DIRECTORY)
        .resolve(parts.directory());

    try {
      Files.createDirectories(finalDir);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create output directory: " + finalDir, e);
    }

    Path basePath = finalDir.resolve(parts.fileNamePrefix());
    switch (outputFormat) {
      case MD -> writeMarkdown(basePath, markdown);
      case HTML -> writeHtml(basePath, markdown, styleName);
      case PDF -> writePdf(scraper, basePath, markdown, styleName);
      default -> throw new IllegalArgumentException("Unknown output format: " + outputFormat);
    }
    writeMetadata(basePath, metadata);
  }

  private void writeMarkdown(Path basePath, String markdown) {
    Path markdownPath = basePath.resolveSibling(basePath.getFileName() + ".md");
    writeFile(markdownPath, markdown);
  }

  private void writeMetadata(Path basePath, String metadata) {
    if (metadata == null || metadata.isBlank()) {
      return;
    }
    Path metadataPath = basePath.resolveSibling(basePath.getFileName() + ".metadata.md");
    writeFile(metadataPath, metadata);
  }

  private void writeHtml(Path basePath, String markdown, String styleName) {
    Path htmlPath = basePath.resolveSibling(basePath.getFileName() + ".html");
    String html = renderer.render(MarkdownUtil.markdownToHtml(markdown), styleName);
    writeFile(htmlPath, html);
    writeMarkdown(basePath, markdown);
    copyHighlightAssets(htmlPath.getParent());
  }

  private void writePdf(Scraper scraper, Path basePath, String markdown, String styleName) {
    Path pdfPath = basePath.resolveSibling(basePath.getFileName() + ".pdf");
    String html = renderer.renderForPdf(MarkdownUtil.markdownToHtml(markdown), styleName);
    copyHighlightAssets(pdfPath.getParent());
    scraper.printPdf(html, pdfPath.toString());
    writeMarkdown(basePath, markdown);
  }

  public void copyHighlightAssets(Path outputDir) {
    if (outputDir == null) {
      return;
    }
    Path assetsDir = outputDir.resolve("assets").resolve("highlight");
    try {
      Files.createDirectories(assetsDir);
      copyResource("assets/highlight/highlight-light.min.css", assetsDir.resolve("highlight-light.min.css"));
      copyResource("assets/highlight/highlight-dark.min.css", assetsDir.resolve("highlight-dark.min.css"));
      copyResource("assets/highlight/highlight.min.js", assetsDir.resolve("highlight.min.js"));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to copy highlight assets", e);
    }
  }

  private void copyResource(String resourcePath, Path target) throws IOException {
    if (Files.exists(target)) {
      return;
    }
    String content = ResourceLoader.readResource(resourcePath);
    Files.writeString(target, content, StandardCharsets.UTF_8);
  }

  private void writeFile(Path path, String content) {
    try {
      Files.writeString(path, content, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write output: " + path, e);
    }
  }

  public record PathParts(String directory, String fileNamePrefix) {}

  public static PathParts pathFromUrl(URL url) {
    String directory = url.getHost().replace("www.", "").replace(".", "-");
    String path = url.getPath();
    if (path == null || path.isBlank() || "/".equals(path)) {
      return new PathParts(directory, "index");
    }
    List<String> segments = List.of(path.split("/"));
    String last = segments.stream().filter(s -> !s.isBlank()).reduce((a, b) -> b).orElse("index");
    return new PathParts(directory, last);
  }
}
