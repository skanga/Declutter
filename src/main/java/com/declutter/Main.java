package com.declutter;

import com.declutter.cli.DeclutterCli;
import picocli.CommandLine;

public final class Main {
  public static void main(String[] args) {
    int exitCode = new CommandLine(new DeclutterCli()).execute(args);
    System.exit(exitCode);
  }
}
