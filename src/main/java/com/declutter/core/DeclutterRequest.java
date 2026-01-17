package com.declutter.core;

import com.declutter.llm.Provider;
import com.declutter.output.OutputFormat;

import java.net.URL;
import java.nio.file.Path;

public record DeclutterRequest(
    URL url,
    OutputFormat outputFormat,
    String styleName,
    Path outputDirectory,
    int maxTokens,
    Provider provider,
    String modelName,
    String browserPath,
    boolean fastMode,
    String geminiKey,
    String openAiKey,
    String openRouterKey,
    String anthropicKey,
    String groqKey,
    String inceptionKey) {}
