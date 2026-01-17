package com.declutter.llm;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;

import java.util.List;

public class LangChain4jLlmClient implements LlmClient {
  private final ChatModel model;

  public LangChain4jLlmClient(ChatModel model) {
    this.model = model;
  }

  @Override
  public LlmResult generate(String systemPrompt, String userPrompt, int maxTokens) {
    ChatRequest request = ChatRequest.builder()
        .messages(List.of(
            SystemMessage.from(systemPrompt),
            UserMessage.from(userPrompt)))
        .maxOutputTokens(maxTokens)
        .build();

    ChatResponse response = model.chat(request);
    String markdown = response.aiMessage().text();
    TokenUsage usage = response.tokenUsage();
    int inputTokens = usage != null && usage.inputTokenCount() != null ? usage.inputTokenCount() : 0;
    int outputTokens = usage != null && usage.outputTokenCount() != null ? usage.outputTokenCount() : 0;
    int totalTokens = usage != null && usage.totalTokenCount() != null ? usage.totalTokenCount() : 0;
    return new LlmResult(markdown, inputTokens, outputTokens, totalTokens);
  }
}
