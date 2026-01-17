package com.declutter.llm;

public record LlmResult(String markdown, int inputTokens, int outputTokens, int totalTokens) {}
