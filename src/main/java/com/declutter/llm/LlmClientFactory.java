package com.declutter.llm;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class LlmClientFactory {
  public LlmClient create(Provider provider, String modelName, int maxTokens, String geminiKey, String openAiKey,
                          String openRouterKey, String anthropicKey, String groqKey, String inceptionKey) {
    String resolvedModel = modelName == null || modelName.isBlank()
        ? ProviderDefaults.DEFAULT_MODELS.get(provider)
        : modelName;

    return switch (provider) {
      case OPENAI -> new LangChain4jLlmClient(openAiModel(openAiKey, resolvedModel, maxTokens));
      case OPENROUTER -> new LangChain4jLlmClient(openRouterModel(openRouterKey, resolvedModel, maxTokens));
      case GROQ -> new LangChain4jLlmClient(groqModel(groqKey, resolvedModel, maxTokens));
      case INCEPTION -> new LangChain4jLlmClient(inceptionModel(inceptionKey, resolvedModel, maxTokens));
      case GEMINI -> new LangChain4jLlmClient(geminiModel(geminiKey, resolvedModel, maxTokens));
      case ANTHROPIC -> new LangChain4jLlmClient(anthropicModel(anthropicKey, resolvedModel, maxTokens));
      case OLLAMA -> new LangChain4jLlmClient(ollamaModel(resolvedModel));
    };
  }

  private ChatModel openAiModel(String apiKey, String modelName, int maxTokens) {
    return OpenAiChatModel.builder()
        .apiKey(apiKey)
        .modelName(modelName)
        .temperature(0.0)
        .maxTokens(maxTokens)
        .build();
  }

  private ChatModel openRouterModel(String apiKey, String modelName, int maxTokens) {
    return OpenAiChatModel.builder()
        .apiKey(apiKey)
        .baseUrl("https://openrouter.ai/api/v1")
        .modelName(modelName)
        .temperature(0.0)
        .maxTokens(maxTokens)
        .build();
  }

  private ChatModel groqModel(String apiKey, String modelName, int maxTokens) {
    return OpenAiChatModel.builder()
        .apiKey(apiKey)
        .baseUrl("https://api.groq.com/openai/v1")
        .modelName(modelName)
        .temperature(0.0)
        .maxTokens(maxTokens)
        .build();
  }

  private ChatModel inceptionModel(String apiKey, String modelName, int maxTokens) {
    return OpenAiChatModel.builder()
        .apiKey(apiKey)
        .baseUrl("https://api.inceptionlabs.ai/v1")
        .modelName(modelName)
        .temperature(0.0)
        .maxTokens(maxTokens)
        .build();
  }

  private ChatModel geminiModel(String apiKey, String modelName, int maxTokens) {
    return GoogleAiGeminiChatModel.builder()
        .apiKey(apiKey)
        .modelName(modelName)
        .temperature(0.0)
        .maxOutputTokens(maxTokens)
        .build();
  }

  private ChatModel anthropicModel(String apiKey, String modelName, int maxTokens) {
    return AnthropicChatModel.builder()
        .apiKey(apiKey)
        .modelName(modelName)
        .temperature(0.0)
        .maxTokens(maxTokens)
        .build();
  }

  private ChatModel ollamaModel(String modelName) {
    return OllamaChatModel.builder()
        .modelName(modelName)
        .temperature(0.0)
        .numCtx(30000)
        .build();
  }
}
