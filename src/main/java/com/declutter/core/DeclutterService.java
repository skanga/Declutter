package com.declutter.core;

import com.declutter.llm.LlmClient;
import com.declutter.llm.LlmClientFactory;
import com.declutter.llm.LlmResult;
import com.declutter.llm.Prompts;
import com.declutter.output.OutputWriter;
import com.declutter.scrape.Scraper;
import com.declutter.util.ConsoleUtil;
import com.declutter.util.MarkdownUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class DeclutterService {
  private final Scraper scraper;
  private final OutputWriter outputWriter;
  private final LlmClientFactory llmClientFactory;

  public DeclutterService(Scraper scraper, OutputWriter outputWriter) {
    this(scraper, outputWriter, new LlmClientFactory());
  }

  public DeclutterService(Scraper scraper, OutputWriter outputWriter, LlmClientFactory llmClientFactory) {
    this.scraper = scraper;
    this.outputWriter = outputWriter;
    this.llmClientFactory = llmClientFactory;
  }

  public void declutter(DeclutterRequest request) {
    boolean needsBrowser = !request.fastMode() || request.outputFormat() == com.declutter.output.OutputFormat.PDF;
    if (needsBrowser && !scraper.isInitialized()) {
      scraper.initialize(request.browserPath());
    }
    String html = scraper.scrapePage(request.url().toString(), request.fastMode());
    String markdown = MarkdownUtil.htmlToMarkdown(html, request.url().getHost());
    String userPrompt = Prompts.inputPrompt(markdown);

    System.out.println("Starting declutter");
    System.out.println("Provider: " + request.provider().id() + " | Model: " + request.modelName());
    LlmClient client = llmClientFactory.create(
        request.provider(),
        request.modelName(),
        request.maxTokens(),
        request.geminiKey(),
        request.openAiKey(),
        request.openRouterKey(),
        request.anthropicKey(),
        request.groqKey(),
        request.inceptionKey());
    LlmResult result = ConsoleUtil.runLoading(
        () -> client.generate(Prompts.SYSTEM_PROMPT, userPrompt, request.maxTokens()),
        "Decluttering content...");
    ConsoleUtil.ok("Decluttering Complete");

    String metadata = metadataTable(request, result);
    String finalMarkdown = result.markdown() == null ? "" : result.markdown();
    finalMarkdown = MarkdownUtil.removeNonImageMarkdownImages(finalMarkdown);

    outputWriter.writeOutput(
        scraper,
        request.url(),
        finalMarkdown,
        metadata,
        request.outputFormat(),
        request.styleName(),
        request.outputDirectory());
  }

  private String metadataTable(DeclutterRequest request, LlmResult result) {
    Map<String, String> rows = new LinkedHashMap<>();
    rows.put("Url", request.url().toString());
    rows.put("Provider", request.provider().id());
    rows.put("Model", request.modelName());
    rows.put("Time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm")));
    rows.put("Input Tokens", String.valueOf(result.inputTokens()));
    rows.put("Output Tokens", String.valueOf(result.outputTokens()));
    rows.put("Total Tokens", String.valueOf(result.totalTokens()));

    StringBuilder sb = new StringBuilder();
    sb.append("| Metadata | Value |\n|-------|-------|\n");
    for (Map.Entry<String, String> entry : rows.entrySet()) {
      sb.append("| ").append(entry.getKey()).append(" | ").append(entry.getValue()).append(" |\n");
    }
    return sb.toString();
  }

  public void close() {
    scraper.close();
  }
}
