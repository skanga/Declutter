package com.declutter.output;

import com.declutter.util.ResourceLoader;

public class TemplateRenderer {
  private static final String HTML_PLACEHOLDER = "__HTML_DATA__";
  private static final String STYLE_PLACEHOLDER = "__STYLE_DATA__";
  private static final String HIGHLIGHT_CSS_PLACEHOLDER = "__HIGHLIGHT_CSS__";
  private static final String HIGHLIGHT_JS_PLACEHOLDER = "__HIGHLIGHT_JS__";
  private static final String TEMPLATE = ResourceLoader.readResource("templates/template.html");
  private static final String HIGHLIGHT_CSS_LIGHT = ResourceLoader.readResource("assets/highlight/highlight-light.min.css");
  private static final String HIGHLIGHT_CSS_DARK = ResourceLoader.readResource("assets/highlight/highlight-dark.min.css");
  private static final String HIGHLIGHT_JS = ResourceLoader.readResource("assets/highlight/highlight.min.js");

  private static final String CDN_BASE = "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0";
  private static final String CDN_CSS_LIGHT = CDN_BASE + "/styles/github.min.css";
  private static final String CDN_CSS_DARK = CDN_BASE + "/styles/github-dark.min.css";
  private static final String CDN_JS = CDN_BASE + "/highlight.min.js";

  public String render(String html, String styleName) {
    return render(html, styleName, false);
  }

  public String renderForPdf(String html, String styleName) {
    return render(html, styleName, true);
  }

  private String render(String html, String styleName, boolean inlineAssets) {
    String style = Styles.STYLES.get(styleName);
    if (style == null) {
      throw new IllegalArgumentException("Unknown style: " + styleName);
    }
    String highlightCss;
    String highlightJs;

    if (inlineAssets) {
      // For PDF: inline all assets for self-contained document
      highlightCss = "<style>\n" + HIGHLIGHT_CSS_LIGHT + "\n</style>\n<style>\n" + HIGHLIGHT_CSS_DARK + "\n</style>";
      highlightJs = "<script>\n" + HIGHLIGHT_JS + "\n</script>\n<script>\nif (window.hljs) { window.hljs.highlightAll(); }\n</script>";
    } else {
      // For HTML: use CDN with local fallback
      highlightCss = String.format("""
          <link rel="stylesheet" href="%s" media="(prefers-color-scheme: light)" \
          onerror="this.onerror=null;this.href='./assets/highlight/highlight-light.min.css';" />
          <link rel="stylesheet" href="%s" media="(prefers-color-scheme: dark)" \
          onerror="this.onerror=null;this.href='./assets/highlight/highlight-dark.min.css';" />""",
          CDN_CSS_LIGHT, CDN_CSS_DARK);
      highlightJs = String.format("""
          <script src="%s" onerror="this.onerror=null;this.src='./assets/highlight/highlight.min.js';"></script>
          <script>if (window.hljs) { window.hljs.highlightAll(); }</script>""",
          CDN_JS);
    }

    return TEMPLATE
        .replace(HTML_PLACEHOLDER, html)
        .replace(STYLE_PLACEHOLDER, style)
        .replace(HIGHLIGHT_CSS_PLACEHOLDER, highlightCss)
        .replace(HIGHLIGHT_JS_PLACEHOLDER, highlightJs);
  }
}
