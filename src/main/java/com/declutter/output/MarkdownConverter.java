package com.declutter.output;

import com.declutter.scrape.Scraper;
import com.declutter.util.MarkdownUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MarkdownConverter {
  private final TemplateRenderer renderer;
  private final OutputWriter outputWriter;

  public MarkdownConverter(TemplateRenderer renderer, OutputWriter outputWriter) {
    this.renderer = renderer;
    this.outputWriter = outputWriter;
  }

  public void convert(Path markdownFilePath, OutputFormat outputFormat, String styleName, String browserPath) {
    if (!markdownFilePath.toString().endsWith(".md")) {
      throw new IllegalArgumentException("invalid markdown file extension, must end with .md");
    }
    if (!Files.exists(markdownFilePath)) {
      throw new IllegalArgumentException("markdown file does not exist: " + markdownFilePath);
    }

    String markdown = readFile(markdownFilePath);
    String html = renderer.render(MarkdownUtil.markdownToHtml(markdown), styleName);

    Path basePath = markdownFilePath.resolveSibling(stripExtension(markdownFilePath.getFileName().toString()));
    if (outputFormat == OutputFormat.HTML) {
      Path htmlPath = basePath.resolveSibling(basePath.getFileName() + ".html");
      writeFile(htmlPath, html);
      outputWriter.copyHighlightAssets(htmlPath.getParent());
      return;
    }
    if (outputFormat == OutputFormat.PDF) {
      Path pdfPath = basePath.resolveSibling(basePath.getFileName() + ".pdf");
      Scraper scraper = new Scraper();
      try {
        scraper.initialize(browserPath);
        outputWriter.copyHighlightAssets(pdfPath.getParent());
        scraper.printPdf(renderer.renderForPdf(MarkdownUtil.markdownToHtml(markdown), styleName), pdfPath.toString());
      } finally {
        scraper.close();
      }
      return;
    }
    throw new IllegalArgumentException("output format must be html or pdf");
  }

  private String stripExtension(String fileName) {
    int idx = fileName.lastIndexOf('.');
    return idx == -1 ? fileName : fileName.substring(0, idx);
  }

  private String readFile(Path path) {
    try {
      return Files.readString(path, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read markdown: " + path, e);
    }
  }

  private void writeFile(Path path, String content) {
    try {
      Files.writeString(path, content, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write output: " + path, e);
    }
  }
}
