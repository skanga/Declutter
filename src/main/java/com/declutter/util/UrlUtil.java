package com.declutter.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public final class UrlUtil {
  private UrlUtil() {}

  public static URL normalize(String value) {
    String normalized = value;
    if (normalized != null && !normalized.matches("^https?://.*")) {
      normalized = "https://" + normalized;
    }
    try {
      return URI.create(normalized).toURL();
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Invalid URL: " + value, e);
    }
  }
}
