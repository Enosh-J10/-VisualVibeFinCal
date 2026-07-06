# FinCalc AI Assistant

The AI Assistant provides personalized financial insights, tips, and conversational support using Google's Gemini Pro model.

## Core Technology
- **Model**: `gemini-1.5-flash` (or current `gemini-flash-latest`).
- **Integration**: Direct Retrofit implementation to the Google Generative Language API.

## Initialization Rules
- **Lazy Load**: AI services (Retrofit, Repository) are initialized only when the user navigates to the AI Chat screen.
- **Startup Protection**: Do not move AI initialization to the Application class or MainActivity.

## Personalities
- **Professional**: Standard helpful financial advice.
- **Roast Mode**: Enabled via settings. Uses a specific system instruction to make the assistant sarcastic and playful about the user's financial habits.

## Data Persistence
- **Local History**: Chat conversations and messages are stored locally in Room (`ConversationEntity`, `MessageEntity`).
- **Privacy**: AI conversations are **not** synced to Firestore in real-time. They are only included in the manual JSON Cloud Backup.

## Safety & Limits
- **Prompt Cap**: User inputs are capped at 5,000 characters.
- **Error Handling**: Handles 429 (Rate Limit) and 503 (Busy) errors with user-friendly messages instead of raw JSON errors.
- **Image Support**: Supports receipt/image analysis via base64 encoding (if enabled in UI).

## Key Classes
- **`AiChatScreen.kt`**: Chat UI with Markdown rendering and voice input.
- **`AiViewModel.kt`**: Manages the conversational state and model switches.
- **`AiRepositoryImpl.kt`**: Handles the actual network requests and local Room inserts.
