package com.declutter.cli;

import com.declutter.output.MarkdownConverter;
import com.declutter.output.OutputFormat;
import com.declutter.output.OutputWriter;
import com.declutter.output.Styles;
import com.declutter.output.TemplateRenderer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;

@Command(name = "convert", description = "Convert a markdown file to html or pdf")
public class ConvertCommand implements Runnable {
  @Parameters(paramLabel = "MARKDOWN", description = "Path to markdown file")
  private Path markdownFilePath;

  @Option(names = {"-f", "--format"}, description = "Output format: html or pdf")
  private String outputFormat = OutputFormat.PDF.id();

  @Option(names = {"-s", "--style"}, description = "Styling of the output")
  private String styleName = Styles.DEFAULT_STYLE;

  @Option(
      names = {"-b", "--browser-path"},
      description = "Path to the Chrome executable")
  private String browserPath;

  @Override
  public void run() {
    OutputFormat format = OutputFormat.from(outputFormat);
    if (format == OutputFormat.MD) {
      throw new IllegalArgumentException("output format must be html or pdf");
    }
    if (!Styles.STYLES.containsKey(styleName)) {
      throw new IllegalArgumentException("style can only be one of: " + Styles.STYLES.keySet());
    }

    MarkdownConverter converter = new MarkdownConverter(
        new TemplateRenderer(),
        new OutputWriter(new TemplateRenderer()));
    converter.convert(markdownFilePath, format, styleName, browserPath);
  }
}
