# Declutter

Declutter is a CLI tool that removes web clutter and produces clean, locally archived content. It extracts a page, runs an LLM-based decluttering pass, and writes Markdown, HTML, or PDF outputs with a choice of styles.

## Features

- Clean Markdown output with metadata
- HTML and PDF rendering with multiple styles
- Offline-ready assets for syntax highlighting
- REPL mode for processing multiple URLs
- Multiple AI providers via LangChain4j

## Requirements

- JDK 25
- Maven 3.9.11+
- A supported LLM provider API key (or local Ollama)
- Playwright will download browsers on first run

## Build

```bash
mvn clean package
mvn clean package -DskipTests 
```

The shaded jar is created at:

```
./target/declutter-0.1.0.jar
```

## Install Playwright Browsers

The first run will auto-download Playwright browsers. If you prefer to preinstall, run a simple command like:

```bash
java -jar target/declutter-0.1.0.jar --help
```

## Usage

### Declutter a URL

```bash
java -jar target/declutter-0.1.0.jar exec https://example.com/article --provider gemini --model-name gemini-2.5-flash --format md
```

Fast mode is the default. It uses a lightweight HTTP fetch first. If the content looks empty, blocked, or low-signal (e.g., bot checks or minimal text), it automatically falls back to Playwright.

To force a full browser render:

```bash
--browser
```

### Choose Output Format

```bash
# Markdown
--format md

# HTML
--format html

# PDF
--format pdf
```

### Choose a Style

```bash
--style MINIMALIST_SWISS
```

Available styles:

- MINIMALIST_SWISS (default)
- BRUTALIST_CONCRETE
- CLASSIC_BOOK
- TECH_TERMINAL
- MINIMALIST_MODERN
- REFINED_ELEGANCE

### Convert Existing Markdown

```bash
java -jar target/declutter-0.1.0.jar convert path\to\file.md --format pdf --style CLASSIC_BOOK
```

### REPL Mode

```bash
java -jar target/declutter-0.1.0.jar repl
```

## Output Directory

By default, outputs go to:

```
~/Documents/Decluttered/<domain>/<slug>.<ext>
```

You can override with:

```bash
--directory C:\path\to\output
```

## Providers

Set one or more API keys as environment variables. The CLI will select the first available key unless `--provider` is specified.

```bash
# Gemini
set GEMINI_API_KEY=your-key

# OpenAI
set OPENAI_API_KEY=your-key

# OpenRouter
set OPENROUTER_API_KEY=your-key

# Anthropic
set ANTHROPIC_API_KEY=your-key

# Groq
set GROQ_API_KEY=your-key

# Inception
set INCEPTION_API_KEY=your-key
```

Supported providers:

- gemini
- openai
- openrouter
- anthropic
- ollama
- groq
- inception

Use a provider/model/key explicitly:

```bash
--provider groq --model-name openai/gpt-oss-20b --groq-key your-key
```

## Custom Browser Path

If Chrome is not found automatically, supply a path:

```bash
--browser-path C:\Program Files\Google\Chrome\Application\chrome.exe
```

## Offline Highlight Assets

The HTML template uses local highlight.js assets located in:

```
src/main/resources/assets/highlight
```

These are copied into each HTML/PDF output directory under:

```
assets/highlight
```

## Troubleshooting

- If a page load times out, try re-running or using a different URL.
- If PDFs fail to render, confirm Playwright browsers are installed.
- If output directories are not writable, supply `--directory` with a valid path.
