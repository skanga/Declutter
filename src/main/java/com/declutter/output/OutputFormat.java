package com.declutter.output;

public enum OutputFormat {
  MD("md"),
  PDF("pdf"),
  HTML("html");

  private final String id;

  OutputFormat(String id) {
    this.id = id;
  }

  public String id() {
    return id;
  }

  public static OutputFormat from(String value) {
    for (OutputFormat format : values()) {
      if (format.id.equalsIgnoreCase(value)) {
        return format;
      }
    }
    throw new IllegalArgumentException("format can only be one of: md, pdf, html");
  }
}
