# Voice Bot for Customer Queries (OpenAI)

Text-in / text-out bot that uses OpenAI to answer **generic telecom** and **user-specific** queries. For full voice, plug in speech-to-text (e.g. Whisper) and text-to-speech (e.g. OpenAI TTS).

## Run

1. Set your OpenAI API key:
   ```bash
   export OPENAI_API_KEY=sk-your-key-here
   ```
2. Run the app:
   ```bash
   mvn compile exec:java -q
   ```
3. Type customer questions (e.g. "What is my balance?", "How do I recharge?") and press Enter. Type `quit` to exit.

## Prompt format for best answers

The bot uses a **system prompt** built from:

1. **Role and rules** – voice-style, short answers, only use provided data  
2. **Knowledge base** – generic product/policy/FAQ (from `knowledge.json`)  
3. **Relevant context only** – AI picks which context (e.g. balance_and_usage, loans) the question relates to; only that context’s schema and customer data are sent  
4. **Safety** – don’t invent info; suggest agent/app if unknown  

See **`PROMPT_FORMAT.md`** for the exact structure, examples, and how to inject your data.

## Code layout

| File | Purpose |
|------|--------|
| `Main.java` | Runs the bot (reads line, calls OpenAI, prints reply) |
| `voicebot/OpenAIService.java` | OpenAI Chat API; classifies context then answers with only that context’s data |
| `voicebot/VoiceBotPromptBuilder.java` | Builds system prompt from knowledge + single-context schema + data |
| `voicebot/ContextCatalog.java` | Context schemas (from `context_schemas.json`) used to classify queries |
| `voicebot/CustomerContextStore.java` | Per-context customer data; use `fromDemoData()` or load from DB by context |

## Customising data

- **Generic knowledge:** Edit `knowledge.json` or point `Main` to your own file.
- **Per-customer:** Use `CustomerContextStore.fromDemoData()` for demo data, or pass a store that fetches only the relevant context from your database (by context name).

## Adding real voice

- **Input:** Send audio to OpenAI Whisper (or another STT), then pass the transcript as `customerQuery`.
- **Output:** Send the bot’s text reply to OpenAI TTS (or another TTS) and play the audio.
