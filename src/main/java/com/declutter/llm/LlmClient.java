package com.declutter.llm;

public interface LlmClient {
  LlmResult generate(String systemPrompt, String userPrompt, int maxTokens);
}
