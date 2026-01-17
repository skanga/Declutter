package com.declutter.llm;

public enum Provider {
  GEMINI("gemini"),
  ANTHROPIC("anthropic"),
  OPENAI("openai"),
  OPENROUTER("openrouter"),
  OLLAMA("ollama"),
  GROQ("groq"),
  INCEPTION("inception");

  private final String id;

  Provider(String id) {
    this.id = id;
  }

  public String id() {
    return id;
  }

  public static Provider from(String value) {
    for (Provider provider : values()) {
      if (provider.id.equalsIgnoreCase(value)) {
        return provider;
      }
    }
    throw new IllegalArgumentException(
        "provider can only be one of: gemini, anthropic, openai, openrouter, ollama, groq, inception");
  }
}
