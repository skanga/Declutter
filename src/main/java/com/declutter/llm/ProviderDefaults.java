package com.declutter.llm;

import java.util.Map;

public final class ProviderDefaults {
  public static final Map<Provider, String> DEFAULT_MODELS = Map.of(
      Provider.GEMINI, "gemini-2.5-flash",
      Provider.ANTHROPIC, "claude-haiku-4-5",
      Provider.OPENAI, "gpt-4o-mini",
      Provider.OPENROUTER, "google/gemini-2.0-flash-exp:free",
      Provider.OLLAMA, "deepseek-r1:7b",
      Provider.GROQ, "llama-3.3-70b-versatile",
      Provider.INCEPTION, "mercury"
  );

  private ProviderDefaults() {}
}
