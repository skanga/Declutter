package com.declutter.cli;

import com.declutter.core.DeclutterRequest;
import com.declutter.core.DeclutterService;
import com.declutter.llm.Provider;
import com.declutter.llm.ProviderDefaults;
import com.declutter.output.OutputFormat;
import com.declutter.output.OutputWriter;
import com.declutter.output.Styles;
import com.declutter.output.TemplateRenderer;
import com.declutter.scrape.Scraper;
import com.declutter.util.UrlUtil;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

@Command(name = "exec", description = "Declutter a given URL into a document")
public class ExecCommand implements Runnable {
  private static final int DEFAULT_MAX_OUTPUT_TOKENS = 10_000;
  private final DeclutterService sharedService;

  public ExecCommand() {
    this.sharedService = null;
  }

  public ExecCommand(DeclutterService sharedService) {
    this.sharedService = sharedService;
  }

  @Parameters(paramLabel = "URL", description = "The URL to declutter")
  private String url;

  @Option(names = {"-t", "--max_tokens"}, description = "Max tokens in LLM output")
  private int maxTokens = DEFAULT_MAX_OUTPUT_TOKENS;

  @Option(
      names = {"-f", "--format"},
      description = "Output format: md, pdf, html")
  private String outputFormat = OutputFormat.PDF.id();

  @Option(
      names = {"-s", "--style"},
      description = "Styling of the output")
  private String styleName = Styles.DEFAULT_STYLE;

  @Option(
      names = {"-d", "--directory"},
      description = "Output directory")
  private Path outputDirectory = CliDefaults.defaultOutputDirectory();

  @Option(
      names = {"-g", "--gemini-key"},
      description = "Gemini API key",
      defaultValue = "${env:GEMINI_API_KEY}")
  private String geminiKey;

  @Option(
      names = {"-o", "--openai-key"},
      description = "OpenAI API key",
      defaultValue = "${env:OPENAI_API_KEY}")
  private String openAiKey;

  @Option(
      names = {"-r", "--open-router-key"},
      description = "OpenRouter API key",
      defaultValue = "${env:OPENROUTER_API_KEY}")
  private String openRouterKey;

  @Option(
      names = {"-a", "--anthropic-key"},
      description = "Anthropic API key",
      defaultValue = "${env:ANTHROPIC_API_KEY}")
  private String anthropicKey;

  @Option(
      names = {"--groq-key"},
      description = "Groq API key",
      defaultValue = "${env:GROQ_API_KEY}")
  private String groqKey;

  @Option(
      names = {"--inception-key"},
      description = "Inception API key",
      defaultValue = "${env:INCEPTION_API_KEY}")
  private String inceptionKey;

  @Option(
      names = {"-m", "--model-name"},
      description = "Model name",
      defaultValue = "${env:DEFAULT_DECLUTTER_MODEL}")
  private String modelName;

  @Option(
      names = {"-p", "--provider"},
      description = "Provider name: gemini, anthropic, openai, openrouter, ollama, groq, inception")
  private String provider;

  @Option(
      names = {"-b", "--browser-path"},
      description = "Path to the Chrome executable")
  private String browserPath;

  @Option(
      names = {"--browser"},
      description = "Force Playwright browser render instead of fast HTTP fetch")
  private boolean browserMode;

  @Override
  public void run() {
    URL normalizedUrl = UrlUtil.normalize(url);
    OutputFormat format = OutputFormat.from(outputFormat);
    if (!Styles.STYLES.containsKey(styleName)) {
      throw new IllegalArgumentException("style can only be one of: " + Styles.STYLES.keySet());
    }
    validateOutputDirectory(outputDirectory);

    Provider resolvedProvider = ProviderResolver.resolve(
        provider,
        geminiKey,
        openAiKey,
        openRouterKey,
        anthropicKey,
        groqKey,
        inceptionKey);

    String resolvedModel = (modelName == null || modelName.isBlank())
        ? ProviderDefaults.DEFAULT_MODELS.get(resolvedProvider)
        : modelName;

    DeclutterRequest request = new DeclutterRequest(
        normalizedUrl,
        format,
        styleName,
        outputDirectory,
        maxTokens,
        resolvedProvider,
        resolvedModel,
        browserPath,
        !browserMode,
        geminiKey,
        openAiKey,
        openRouterKey,
        anthropicKey,
        groqKey,
        inceptionKey);

    if (sharedService != null) {
      sharedService.declutter(request);
      return;
    }

    DeclutterService service = new DeclutterService(new Scraper(), new OutputWriter(new TemplateRenderer()));
    try {
      service.declutter(request);
    } finally {
      service.close();
    }
  }

  private void validateOutputDirectory(Path outputDirectory) {
    if (outputDirectory == null) {
      throw new IllegalArgumentException("Output directory cannot be null");
    }
    if (!Files.exists(outputDirectory) || !Files.isDirectory(outputDirectory)) {
      throw new IllegalArgumentException(
          "Cannot generate output in " + outputDirectory + " directory please provide write permissions");
    }
    if (!Files.isWritable(outputDirectory)) {
      throw new IllegalArgumentException(
          "Cannot generate output in " + outputDirectory + " directory please provide write permissions");
    }
  }
}
