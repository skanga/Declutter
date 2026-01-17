package com.declutter.cli;

import com.declutter.core.DeclutterService;
import com.declutter.output.OutputWriter;
import com.declutter.output.TemplateRenderer;
import com.declutter.scrape.Scraper;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

@Command(name = "repl", description = "Start declutter in REPL mode")
public class ReplCommand implements Runnable {
  @Override
  public void run() {
    System.out.println("Welcome to declutter REPL mode!");
    System.out.println("Type 'exit' or press Ctrl+C to quit at any time.");

    DeclutterService sharedService = new DeclutterService(new Scraper(), new OutputWriter(new TemplateRenderer()));
    CommandLine execCommand = new CommandLine(new ExecCommand(sharedService));
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
      while (true) {
        System.out.print("> ");
        String line = reader.readLine();
        if (line == null) {
          break;
        }
        String trimmed = line.trim();
        if (trimmed.equalsIgnoreCase("exit")) {
          System.out.println("Goodbye! Exiting REPL.");
          break;
        }
        if (trimmed.isBlank()) {
          continue;
        }
        String[] args = Arrays.stream(trimmed.split(" "))
            .filter(s -> !s.isBlank())
            .toArray(String[]::new);
        execCommand.execute(args);
      }
    } catch (IOException e) {
      throw new IllegalStateException("REPL error", e);
    } finally {
      sharedService.close();
    }
  }
}
