package com.declutter.cli;

import java.nio.file.Path;

public final class CliDefaults {
  private CliDefaults() {}

  public static Path defaultOutputDirectory() {
    String userHome = System.getProperty("user.home");
    return Path.of(userHome, "Documents");
  }
}
