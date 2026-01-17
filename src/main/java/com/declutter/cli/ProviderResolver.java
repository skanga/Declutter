package com.declutter.cli;

import com.declutter.llm.Provider;

public final class ProviderResolver {
  private ProviderResolver() {}

  public static Provider resolve(
      String provider,
      String geminiKey,
      String openAiKey,
      String openRouterKey,
      String anthropicKey,
      String groqKey,
      String inceptionKey) {
    if (provider != null && !provider.isBlank()) {
      Provider selected = Provider.from(provider);
      switch (selected) {
        case GEMINI -> {
          if (isBlank(geminiKey)) {
            throw new IllegalArgumentException("provider set to gemini but no API key provided");
          }
          return Provider.GEMINI;
        }
        case ANTHROPIC -> {
          if (isBlank(anthropicKey)) {
            throw new IllegalArgumentException("provider set to anthropic but no API key provided");
          }
          return Provider.ANTHROPIC;
        }
        case OPENAI -> {
          if (isBlank(openAiKey)) {
            throw new IllegalArgumentException("provider set to openai but no API key provided");
          }
          return Provider.OPENAI;
        }
        case OPENROUTER -> {
          if (isBlank(openRouterKey)) {
            throw new IllegalArgumentException("provider set to openrouter but no API key provided");
          }
          return Provider.OPENROUTER;
        }
        case GROQ -> {
          if (isBlank(groqKey)) {
            throw new IllegalArgumentException("provider set to groq but no API key provided");
          }
          return Provider.GROQ;
        }
        case INCEPTION -> {
          if (isBlank(inceptionKey)) {
            throw new IllegalArgumentException("provider set to inception but no API key provided");
          }
          return Provider.INCEPTION;
        }
        case OLLAMA -> {
          return Provider.OLLAMA;
        }
        default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
      }
    }

    if (!isBlank(geminiKey)) {
      return Provider.GEMINI;
    }
    if (!isBlank(openAiKey)) {
      return Provider.OPENAI;
    }
    if (!isBlank(openRouterKey)) {
      return Provider.OPENROUTER;
    }
    if (!isBlank(groqKey)) {
      return Provider.GROQ;
    }
    if (!isBlank(inceptionKey)) {
      return Provider.INCEPTION;
    }
    if (!isBlank(anthropicKey)) {
      return Provider.ANTHROPIC;
    }

    throw new IllegalArgumentException("No API key provided. Set GEMINI_API_KEY, OPENAI_API_KEY, OPENROUTER_API_KEY, GROQ_API_KEY, INCEPTION_API_KEY, or ANTHROPIC_API_KEY (or use ollama).");
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
