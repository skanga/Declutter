package com.declutter.scrape;

import com.declutter.util.ConsoleUtil;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.Margin;
import com.microsoft.playwright.options.WaitUntilState;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Scraper {
  private static final Random RANDOM = new Random();
  private static final double DEFAULT_TIMEOUT_MS = 30000;
  private static final String USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
  private static final String STEALTH_SCRIPT = """
      Object.defineProperty(navigator, 'webdriver', { get: () => undefined });
      window.chrome = { runtime: {} };
      const originalQuery = window.navigator.permissions.query;
      window.navigator.permissions.query = (parameters) =>
        parameters.name === 'notifications'
          ? Promise.resolve({ state: Notification.permission })
          : originalQuery(parameters);
      Object.defineProperty(navigator, 'plugins', {
        get: () => [
          {
            0: { type: 'application/x-google-chrome-pdf', suffixes: 'pdf', description: 'Portable Document Format', enabledPlugin: Plugin },
            description: 'Portable Document Format',
            filename: 'internal-pdf-viewer',
            length: 1,
            name: 'Chrome PDF Plugin',
          },
          {
            0: { type: 'application/pdf', suffixes: 'pdf', description: '', enabledPlugin: Plugin },
            description: '',
            filename: 'mhjfbmdgcfjbbpaeojofohoefgiehjai',
            length: 1,
            name: 'Chrome PDF Viewer',
          },
          {
            0: { type: 'application/x-nacl', suffixes: '', description: 'Native Client Executable', enabledPlugin: Plugin },
            1: { type: 'application/x-pnacl', suffixes: '', description: 'Portable Native Client Executable', enabledPlugin: Plugin },
            description: '',
            filename: 'internal-nacl-plugin',
            length: 2,
            name: 'Native Client',
          },
        ],
      });
      Object.defineProperty(navigator, 'languages', { get: () => ['en-US', 'en'] });
      const originalToString = Function.prototype.toString;
      Function.prototype.toString = function () {
        if (this === window.navigator.permissions.query) {
          return 'function query() { [native code] }';
        }
        return originalToString.call(this);
      };
      Object.defineProperty(navigator, 'platform', { get: () => 'Win32' });
      Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => 8 });
      Object.defineProperty(navigator, 'deviceMemory', { get: () => 8 });
      const originalContentWindow = Object.getOwnPropertyDescriptor(HTMLIFrameElement.prototype, 'contentWindow');
      if (originalContentWindow) {
        Object.defineProperty(HTMLIFrameElement.prototype, 'contentWindow', {
          get: function () {
            const win = originalContentWindow.get?.call(this);
            if (win) {
              try { win.navigator.webdriver = false; } catch {}
            }
            return win;
          },
        });
      }
      let mouseX = 0;
      let mouseY = 0;
      const updateMousePosition = () => {
        mouseX += (Math.random() - 0.5) * 10;
        mouseY += (Math.random() - 0.5) * 10;
        mouseX = Math.max(0, Math.min(window.innerWidth, mouseX));
        mouseY = Math.max(0, Math.min(window.innerHeight, mouseY));
      };
      setInterval(updateMousePosition, 100);
      """;
  private Playwright playwright;
  private Browser browser;

  public void initialize(String browserPath) {
    if (browser != null) {
      return;
    }
    playwright = Playwright.create();
    BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
        .setHeadless(true)
        .setArgs(List.of(
            "--no-sandbox",
            "--disable-setuid-sandbox",
            "--disable-blink-features=AutomationControlled",
            "--disable-infobars",
            "--window-size=1920,1080",
            "--disable-web-security",
            "--disable-features=IsolateOrigins,site-per-process",
            "--disable-dev-shm-usage",
            "--disable-accelerated-2d-canvas",
            "--no-first-run",
            "--no-zygote",
            "--disable-gpu",
            "--lang=en-US,en;q=0.9"
        ))
        .setIgnoreDefaultArgs(List.of("--enable-automation"))
        .setTimeout(DEFAULT_TIMEOUT_MS);
    if (browserPath != null && !browserPath.isBlank()) {
      options.setExecutablePath(Path.of(browserPath));
    } else {
      options.setChannel("chrome");
    }
    browser = playwright.chromium().launch(options);
  }

  public boolean isInitialized() {
    return browser != null;
  }

  public String scrapePage(String url) {
    ensureInitialized();
    try (BrowserContext context = newContext()) {
      Page page = context.newPage();
      ConsoleUtil.ok("Starting Page fetch");
      page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
      waitForNetworkIdle2(page, 2, 500, (int) DEFAULT_TIMEOUT_MS);
      ConsoleUtil.ok("Network idle completed");

      randomDelay(500, 1500);
      page.mouse().move(RANDOM.nextInt(1000), RANDOM.nextInt(800));
      randomDelay(300, 800);
      randomScroll(page);
      randomDelay(500, 1000);
      ConsoleUtil.ok("Final random delay completed");
      return page.content();
    }
  }

  public String scrapePage(String url, boolean fastMode) {
    if (!fastMode) {
      return scrapePage(url);
    }
    ConsoleUtil.ok("Starting fast fetch");
    try {
      String html = fetchWithHttp(url);
      if (!isLikelyUsableHtml(html)) {
        ConsoleUtil.warn("Fast fetch returned low-signal content. Falling back to browser render.");
        return scrapePage(url);
      }
      return html;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      ConsoleUtil.warn("Fast fetch interrupted. Falling back to browser render.");
      return scrapePage(url);
    } catch (IOException e) {
      ConsoleUtil.warn("Fast fetch failed. Falling back to browser render.");
      return scrapePage(url);
    }
  }

  public void printPdf(String htmlContent, String documentPath) {
    ensureInitialized();
    try (BrowserContext context = newContext()) {
      Page page = context.newPage();
      page.setContent(htmlContent, new Page.SetContentOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
      page.pdf(new Page.PdfOptions()
          .setPath(Path.of(documentPath))
          .setFormat("A4")
          .setPrintBackground(true)
          .setMargin(new Margin()
              .setTop("50px")
              .setRight("50px")
              .setBottom("50px")
              .setLeft("50px")));
    }
  }

  private BrowserContext newContext() {
    BrowserContext context = browser.newContext(new Browser.NewContextOptions()
        .setUserAgent(USER_AGENT)
        .setViewportSize(1920, 1080));

    context.setDefaultNavigationTimeout(DEFAULT_TIMEOUT_MS);
    context.setDefaultTimeout(DEFAULT_TIMEOUT_MS);
    context.addInitScript(STEALTH_SCRIPT);
    context.setExtraHTTPHeaders(defaultHeaders());
    return context;
  }

  private String fetchWithHttp(String url) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .GET()
        .header("User-Agent", USER_AGENT)
        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        .header("Accept-Language", "en-US,en;q=0.9")
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() >= 400) {
      return "";
    }
    return response.body();
  }

  private boolean isLikelyUsableHtml(String html) {
    if (html == null || html.isBlank()) {
      return false;
    }
    String lower = html.toLowerCase();
    if (lower.contains("enable javascript") || lower.contains("access denied")
        || lower.contains("bot detection") || lower.contains("captcha")
        || lower.contains("are you human")) {
      return false;
    }
    Document doc = Jsoup.parse(html);
    doc.select("script, style, iframe, object, embed, noscript").remove();
    String text = doc.body() == null ? "" : doc.body().text();
    int textLength = text == null ? 0 : text.trim().length();
    boolean hasStructure = doc.selectFirst("main, article, h1") != null;
    return textLength >= 400 || hasStructure;
  }

  private void waitForNetworkIdle2(Page page, int maxInflight, int idleMs, int timeoutMs) {
    AtomicInteger inflight = new AtomicInteger(0);
    page.onRequest(request -> inflight.incrementAndGet());
    page.onRequestFinished(request -> inflight.decrementAndGet());
    page.onRequestFailed(request -> inflight.decrementAndGet());

    long start = System.currentTimeMillis();
    long idleStart = -1;
    while (System.currentTimeMillis() - start < timeoutMs) {
      int current = inflight.get();
      if (current <= maxInflight) {
        if (idleStart < 0) {
          idleStart = System.currentTimeMillis();
        } else if (System.currentTimeMillis() - idleStart >= idleMs) {
          return;
        }
      } else {
        idleStart = -1;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    }
    throw new com.microsoft.playwright.TimeoutError("Timeout " + timeoutMs + "ms exceeded waiting for network idle");
  }

  private Map<String, String> defaultHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Accept-Language", "en-US,en;q=0.9");
    headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
    headers.put("Accept-Encoding", "gzip, deflate, br");
    headers.put("Connection", "keep-alive");
    headers.put("Upgrade-Insecure-Requests", "1");
    headers.put("Sec-Fetch-Dest", "document");
    headers.put("Sec-Fetch-Mode", "navigate");
    headers.put("Sec-Fetch-Site", "none");
    headers.put("Cache-Control", "max-age=0");
    return headers;
  }

  private void randomDelay(int minMs, int maxMs) {
    int delay = RANDOM.nextInt(maxMs - minMs + 1) + minMs;
    try {
      Thread.sleep(delay);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void randomScroll(Page page) {
    page.evaluate("""
        () => {
          const distance = Math.floor(Math.random() * 500) + 100;
          const delay = Math.floor(Math.random() * 100) + 50;
          return new Promise(resolve => {
            let totalHeight = 0;
            const timer = setInterval(() => {
              window.scrollBy(0, distance / 10);
              totalHeight += distance / 10;
              if (totalHeight >= distance) {
                clearInterval(timer);
                resolve();
              }
            }, delay);
          });
        }
        """);
  }

  private void ensureInitialized() {
    if (browser == null) {
      initialize(null);
    }
  }

  public void close() {
    if (browser != null) {
      browser.close();
      browser = null;
    }
    if (playwright != null) {
      playwright.close();
      playwright = null;
    }
  }
}
