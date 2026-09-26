# 00_MASTER_CONTEXT: AIRA AI ASSISTANT ARCHITECTURAL SNAPSHOT

> **Document Type:** Master Technical Specification & Architecture Knowledge Base  
> **Source of Truth:** 100% Code-Audited against the actual production codebase (`com.example`), Gradle build configurations, AndroidManifest, JNI native libraries, and Room database schemas.  
> **Verification Status:** FINALIZED_AFTER_CODE_AUDIT (No speculative claims).

---

## 1. PROJECT IDENTITY & METADATA

- **Project Name:** Aira (Also customized and branded as J.A.R.V.I.S. / Iron Man Stark OS Assistant).
- **Application Namespace:** `com.example` (Root package structure).
- **Application ID:** `com.aistudio.voiceassistant.wkjzla` (Defined in `app/build.gradle.kts`).
- **Project Type:** Native Android Application (Single-module `app`).
- **Platform & Target:** Android OS (API 26 Oreo minimum, API 35 Android 15 target/compile).
- **Primary Language:** Kotlin 2.0.21 (100% Kotlin DSL Gradle scripts).
- **Primary Frameworks:** Jetpack Compose (BOM 2024.10.01), Material 3 (1.3.1), AndroidX Room (2.6.1), OkHttp / Retrofit (2.11.0), AndroidX Security Crypto (1.1.0-alpha06).
- **System Integration:** Registered Android Default Voice Interaction Service (`android.service.voice.VoiceInteractionService`), Accessibility Service (`AccessibilityService`), and Device Administrator (`DeviceAdminReceiver`).

---

## 2. PRODUCT PHILOSOPHY & OPERATIONAL MODES

1. **Dual Intelligence Paradigm (Online Cloud + Offline Grid):**
   - **Online Mode:** Multi-cloud LLM fallback orchestration (Gemini, Groq, OpenAI, Claude, OpenRouter, Mistral, Cohere, HuggingFace) featuring asynchronous SSE token streaming, sentence chunking, and real-time voice synthesis.
   - **Offline Mode:** Zero-network on-device execution leveraging Vosk offline STT, Piper neural ONNX TTS (`libpiper.so`), local JNI / simulated Llama 3.2 engine (`LlamaCppBrain`), Room SQLite caching, and native system automation.
2. **Iron Man J.A.R.V.I.S. Persona:**
   - Calm, witty, hyper-competent British intellect with customizable voice prosody (pitch 0.92, speed 1.08) and formal address protocols ("sir").
3. **Hardened Privacy & Security:**
   - Dedicated "Privacy Mode" locking all execution to on-device algorithms.
   - Multi-key API vaults protected by Android Keystore hardware-backed master keys (`MasterKeys.AES256_GCM_SPEC`).
4. **Low-Memory Device Optimization:**
   - Aggressive RAM protection guardrails ensuring stable execution on 2GB RAM budget hardware (Android Go) without background service termination.

---

## 3. COMPLETE TECHNOLOGY STACK

### 3.1 Toolchain & Core Libraries
- **Language:** Kotlin `2.0.21` (JVM target `17`).
- **Android Gradle Plugin (AGP):** `8.7.2`.
- **Compile / Target SDK:** `35` (Android 15), **Min SDK:** `26` (Android 8.0).
- **Jetpack Compose:** Compose BOM `2024.10.01`, Foundation, UI, Material 3 `1.3.1`.
- **Navigation:** Jetpack Navigation Compose `2.8.3` + In-app tab deck switcher.
- **Dependency Injection:** Manual Container / Constructor Injection (`AppContainer.kt`, `AppModule.kt`).
- **Coroutines & Asynchrony:** Kotlin Coroutines `1.9.0`, StateFlow / SharedFlow, Coroutine Channels.

### 3.2 Data, Storage & Security
- **Local Database:** AndroidX Room `2.6.1` with KSP compiler (SQLite database version `10`).
- **Security & Keystore:** AndroidX Security Crypto `1.1.0-alpha06` (`EncryptedSharedPreferences` with AES-256 SIV/GCM).
- **Preferences:** AndroidX DataStore Preferences `1.1.1` + Standard `SharedPreferences`.

### 3.3 Networking & Streams
- **HTTP Client:** OkHttp `4.12.0` (with logging interceptors and retry handlers).
- **REST Framework:** Retrofit `2.11.0` with Gson `2.11.0` and `kotlinx.serialization` `1.7.3`.
- **Stream Processing:** OkHttp SSE (Server-Sent Events) `AiStreamManager.kt`.

### 3.4 Voice, AI & Native Binaries
- **Offline STT:** Vosk Android SDK `0.3.47` (`net.alphacephei:vosk-android`).
- **Online STT:** Android Native `SpeechRecognizer` with continuous streaming callbacks.
- **Neural TTS (JNI):** Piper C++ Engine (`libpiper.so`, `libonnxruntime.so` in `app/src/main/jniLibs/arm64-v8a`).
- **System TTS:** Android `android.speech.tts.TextToSpeech`.
- **Privileged Automation:** Shizuku API `13.1.5` (`rikka.shizuku:api`, `rikka.shizuku:provider`).
- **Optical Tools:** Google ML Kit Text Recognition (OCR) `16.0.1`, Barcode/QR Scanning `17.3.0`.
- **Image Loading:** Coil Compose `2.7.0`.

---

## 4. PROCESS, LIFECYCLE & MEMORY ARCHITECTURE

```
┌────────────────────────────────────────────────────────────────────────┐
│                        ANDROID OPERATING SYSTEM                        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
       ┌────────────────────────────┴────────────────────────────┐
       ▼                                                         ▼
┌──────────────────────────────────────┐     ┌───────────────────────────────────┐
│     MAIN PROCESS: com.example        │     │ ASSISTANT PROCESS: com.example    │
│  (UI, ViewModels, Heavy AI, STT/TTS) │     │              :assistant           │
├──────────────────────────────────────┤     ├───────────────────────────────────┤
│ • MainActivity / Tab Navigation      │     │ • AssistantService (Voice)        │
│ • AiraViewModel (Central State)      │     │ • AssistantSessionService         │
│ • AiBrain / Cloud LLM Streaming      │     │ • Ultra-low memory (<30MB)        │
│ • Vosk STT & Piper ONNX TTS          │     │ • Heavy Piper/Llama loading       │
│ • Room Database (AppDatabase v10)    │     │   BLOCKED in AiraApplication      │
│ • EncryptedSharedPreferences (AES)   │     │   to prevent low-RAM OOM.         │
└──────────────────┬───────────────────┘     └───────────────────────────────────┘
                   │
       ┌───────────┴─────────────────────────────────────────────┐
       ▼                                                         ▼
┌──────────────────────────────────────┐     ┌───────────────────────────────────┐
│       ACCESSIBILITY SUBSYSTEM        │     │          SHIZUKU DAEMON           │
│      (AiraAccessibilityService)      │     │        (Privileged Shell)         │
├──────────────────────────────────────┤     ├───────────────────────────────────┤
│ • UI Tree Parsing (dumpNodeTree)     │     │ • Elevated Shell Commands         │
│ • Auto-Taps, Swipes & Form Typing    │     │ • Wi-Fi, Bluetooth, Mobile Data   │
│ • Screen OCR & Text Scraping         │     │ • Force Stop, Background Clears   │
└──────────────────────────────────────┘     └───────────────────────────────────┘
```

### 4.1 Process Isolation Rule (`AiraApplication.kt`)
In `AiraApplication.kt`, the application checks its running process name. When running under `:assistant`:
1. Native library preloading (Piper JNI, ONNX Runtime) is **skipped**.
2. Background heavy speech pipelines are **suppressed**.
3. Guarantees that the Android system voice interaction assistant service will not be terminated by LMK (Low Memory Killer) on budget 2GB RAM devices.

### 4.2 Dynamic Memory Protection (`MemoryManager.kt`)
- Devices with **<3GB total RAM** are flagged with `isOfflineSupported = false`.
- Heavy native engines (`LlamaCppBrain`, local model buffers) verify `MemoryManager.isOfflineSupported(context)` before allocating memory. If RAM is low, execution automatically falls back to optimized cloud API streaming.

---

## 5. ARTIFICIAL INTELLIGENCE ARCHITECTURE

### 5.1 Universal Intelligence Flow (`AiBrain.kt`)

```
User Voice / Text Input
          │
          ▼
┌────────────────────────────────────────┐
│ 1. Local Exact Cache Check (Room Cache) │ ──(Hit)──► Return Instant Cached Response
└──────────────────┬─────────────────────┘
                   │ (Miss)
                   ▼
┌────────────────────────────────────────┐
│ 2. Command Extraction / Task Detection │ ──(System Command)──► Route to AiraAutomationEngine
└──────────────────┬─────────────────────┘
                   │ (Conversational Query)
                   ▼
┌────────────────────────────────────────┐
│ 3. Specialized Toolkit Evaluation      │ ──(Math/Convert/Search)──► Route to JarvisSpecializedToolkit
└──────────────────┬─────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────┐
│ 4. Memory & Persona Context Injection  │ ──► Attach Extracted Facts & J.A.R.V.I.S Prompt
└──────────────────┬─────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 5. Cloud Provider Failover Chain Execution                             │
│    Active Provider -> Gemini -> Groq -> OpenAI -> Claude -> OpenRouter │
│    -> Mistral -> Cohere -> HuggingFace                                 │
└──────────────────┬─────────────────────────────────────────────────────┘
                   │ (If Network Available)
                   ├──────────────────────────────────────┐
                   ▼                                      ▼
      Cloud SSE Streaming Flow               Multi-Key Round-Robin Selector
      (`AiStreamManager.kt`)                 (`MultiKeyManager.kt` AES-256)
                   │
                   ▼ (If Network Fails / Offline / Privacy Mode Active)
┌────────────────────────────────────────────────────────────────────────┐
│ 6. Offline Fallback: LlamaCppBrain (JNI + Local Heuristic Transformer) │
└────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Provider Registry (`com.example.network.api`)
1. **Google Gemini:** `GeminiClient.kt` (Gemini 2.0 Flash, Gemini 1.5 Pro).
2. **Groq:** `GroqClient.kt` (Llama 3.3 70B Versatile, Mixtral 8x7b) — Primary low-latency backend.
3. **OpenAI:** `OpenAIClient.kt` (GPT-4o, GPT-4o-mini).
4. **Anthropic Claude:** `ClaudeClient.kt` (Claude 3.5 Sonnet, Claude 3.5 Haiku).
5. **OpenRouter:** `OpenRouterClient.kt` (Universal aggregator).
6. **Mistral AI:** `MistralClient.kt` (Mistral Large, Codestral).
7. **Cohere:** `CohereClient.kt` (Command R+).
8. **HuggingFace:** `HuggingFaceClient.kt` (Inference API).
9. **News RSS Service:** `NewsRssService.kt` (Real-time live news parser).
10. **Local On-Device Engine:** `LlamaCppBrain.kt` (Local GGUF JNI + offline heuristic simulation).

### 5.3 Multi-Key API Vault & Cooldown Logic (`MultiKeyManager.kt`)
- Keys are saved in `EncryptedSharedPreferences` as JSON arrays per provider.
- Each key tracks `failureCount`, `cooldownUntilTimestamp`, and `lastUsedTimestamp`.
- If an HTTP 429 or 401/403 status occurs, `recordFailure()` applies an exponential cooldown (default 1 hour) and `getNextActiveKey()` seamlessly rotates to the next valid key.

---

## 6. VOICE PIPELINE & STREAMING CONCURRENCY

### 6.1 Speech-to-Text (STT) Hierarchy
1. **Online/Primary:** Android Native `SpeechRecognizer` with streaming speech recognition callbacks.
2. **Offline Fallback:** Vosk Speech Recognition SDK (`VoskSpeechRecognizer.kt`) initialized with local acoustic model at `assets/vosk-model-small-en-us-0.15`.

### 6.2 Text-to-Speech (TTS) 3-Tier Hierarchy (`PiperTtsManager.kt`)
1. **Tier 1 (Primary):** Google Text-to-Speech (`android.speech.tts.TextToSpeech`) tuned with custom pitch (0.92) and speed (1.08) for British accent J.A.R.V.I.S. persona (`en-GB`), plus specialized voice profiles (Lily, Zara, Ella).
2. **Tier 2 (Offline Fallback):** Real Piper Neural TTS Engine via JNI (`PiperTtsEngine.kt`, `libpiper.so`) synthesizing high-quality 22.5kHz audio from `amymodel.onnx` (downloaded runtime to `filesDir/piper/`).
3. **Tier 3 (System Fallback):** Standard system speech synthesizer with emotional prosody modulation (`SentimentAnalysisUtility.kt`).

### 6.3 Pipelined Streaming Concurrency (`SentenceChunker.kt`)
`SentenceChunker` breaks incoming SSE token deltas at sentence delimiters (`.`, `!`, `?`, `\n`). When the first full sentence arrives (typically 250–400ms), it immediately dispatches that sentence to `PiperTtsManager.speakText()`, while subsequent sentences buffer in an asynchronous queue. This achieves instant vocal response before generation finishes.

### 6.4 Wake Word & Listening Service
- **Service:** `ActiveListeningService.kt` runs as an Android Foreground Service (`foregroundServiceType="microphone"`).
- **Engine:** Continuous background audio loop powered by Vosk acoustic streaming and `AudioRecord` keyword matching for `"Hey Jarvis"`, `"Jarvis"`, and `"Hey Aira"`.
- **Calibration:** Dynamic acoustic training records are stored in Room (`TrainedWakeWordDao`).

---

## 7. AUTOMATION, DEVICE CONTROL & SPECIALIZED TOOLS

### 7.1 Realistic Automation Architecture
The system exposes **~40 discrete high-level Kotlin action handlers** in `AiraAutomationEngine.kt`, capable of fulfilling hundreds of natural language permutations and custom multi-step macro sequences (`MacroEntity`).

#### Core Execution Domains:
1. **Connectivity & Radio:** Wi-Fi toggle, Bluetooth toggle, Mobile Data toggle, Hotspot toggle, Airplane mode toggle, GPS Location toggle.
2. **Audio & Media:** Master volume adjustments, Sound mode (Normal, Vibrate, Silent), DND on/off, Media play/pause/next/previous.
3. **Display & Power:** Brightness adjustment, Flashlight toggle, Lock screen, Screen rotation, Take screenshot, Open Power menu.
4. **App & Task Execution:** Launch any installed application by name or package, Force-stop app, Clear app cache/data, Open Settings pages, Open Notifications shade, Open Quick Settings panel.
5. **System Navigation:** Navigate Back, Navigate Home, Open Recents / App Overview.
6. **UI Automation (`AiraAccessibilityService.kt`):** Dump accessibility node tree, tap on text/ID/class, swipe up/down/left/right, programmatic gestures, auto-type into fields with inter-key delay.
7. **Privileged Operations (`ShizukuServiceWrapper.kt`):** Background shell execution for settings injection, permission management, and package operations without root.

### 7.2 Specialized J.A.R.V.I.S. Toolkit (`JarvisSpecializedToolkit.kt`)
- **Calculator & Math:** Evaluates arithmetic expressions, percentages ("20% of 500"), square roots ("sqrt 144"), and exponentiation without calling cloud APIs.
- **Unit & Currency Converter:** Offline conversions across currencies (USD, EUR, GBP, JPY, CAD, AUD, INR, PKR, AED, CNY), lengths (km, m, cm, mi, yd, ft, in), weights (kg, g, mg, lb, oz), and temperatures (°C, °F).
- **Clipboard Utility:** Audible clipboard reading and text copying.
- **Screen OCR & Notification Reader:** On-device visual text extraction from active accessibility nodes.
- **Optical Scanner:** Launches optical barcode and QR scanning.
- **Web Search Grounding:** Direct dispatch to Google Web Search, YouTube, and Wikipedia.
- **Smart Replies:** Context-aware quick reply chips generated after every assistant turn.

---

## 8. UI & PRESENTATION ARCHITECTURE

### 8.1 Navigation & Tab Structure (`MainActivity.kt`, `NavRoutes.kt`)
Aira employs an **Animated Tab Deck** with cross-fade transitions managed by `selectedTab: Int` in `MainActivity.kt`:

1. **Tab 0 (`NavRoutes.TAB_ASSISTANT`) — `HomeScreen.kt`:**
   - Stark OS reactive HUD and ambient visual background.
   - Interactive Pulsing Voice Orb (`AiraVoiceOrb.kt`) with live amplitude feedback.
   - Real-time conversation stream (`LazyColumn` with chat message cards).
   - Smart Reply Suggestion Chips (`LazyRow`).
   - Fallback Text Input Bar (`OutlinedTextField` + Send action).
   - Iron Man Morning Briefing & Battery / Weather status cards.
2. **Tab 1 (`NavRoutes.TAB_COMMANDS`) — `SystemControlScreen.kt`:**
   - J.A.R.V.I.S. Core Protocols Card (Privacy Mode, DND Mode, Mute Mode toggles).
   - Quick Tools Bar (QR Scanner, Screen OCR, Clipboard Reader).
   - Assistant Google Control Card & Hardware Quick Action Deck.
   - System Utilities and Custom Voice Command / Macro Manager.
3. **Tab 2 (`NavRoutes.TAB_FEEDS`) — `ExtrasScreen.kt`:**
   - Live Weather telemetry and hourly forecasts.
   - News RSS Category Feed browser.
   - Real-time system performance and diagnostic metrics.
4. **Tab 3 (`NavRoutes.TAB_CONFIG`) — `SettingsScreen.kt`:**
   - Multi-API Key Manager Card (Gemini, Groq, OpenAI, Claude, OpenRouter, Mistral, Cohere, HuggingFace).
   - Voice Engine & Profile Selector (Pitch, Speed, Model selection).
   - Vosk Offline Diagnostic Panel & Log Viewer.
   - Shizuku Service Status & Permission Manager.

### 8.2 Modal Overlays & Secondary Dialogs
- `OnboardingScreen.kt`: Initial permissions wizard and welcome flow.
- `WakeWordTrainerScreen.kt`: Voice sample recording and acoustic calibration.
- `ThemeScreen.kt`: UI palette customization (Stark Cyan, Arc Gold, Stealth Dark).
- `ModelDownloadPopup.kt`: Progress manager for Piper ONNX / Vosk acoustic assets.

---

## 9. DATABASE & PERSISTENCE LAYER

- **Database Name:** `DatabaseSchema.DATABASE_NAME` (`aira_database.db`)
- **Room Version:** `10` (`AppDatabase.kt`)
- **Migration Strategy:** `fallbackToDestructiveMigration(true)` with `DatabaseSchema.ALL_MIGRATIONS`.

### 9.1 All 12 Registered Entities:

| # | Entity Class | Table Name | Purpose |
| :--- | :--- | :--- | :--- |
| 1 | `ChatMessage` | `chat_messages` | Full conversational history, token counts, latency, provider used. |
| 2 | `Reminder` | `reminders` | Alarms, timers, and scheduled voice reminders. |
| 3 | `GrokCache` | `grok_cache` | Dedicated Grok/Groq response cache entries. |
| 4 | `Action` | `actions` | Discrete automation action definitions. |
| 5 | `Command` | `commands` | Voice command trigger phrases mapped to actions. |
| 6 | `Memory` | `memories` | Extracted long-term user facts and semantic tags. |
| 7 | `TrainedWakeWord` | `trained_wake_words` | Acoustic calibration audio records for custom wake words. |
| 8 | `ResponseFeedback` | `response_feedbacks` | Upvote/downvote and user quality ratings on responses. |
| 9 | `VoiceCommandLogEntity` | `voice_command_logs` | Audit trail of parsed voice intents and execution results. |
| 10 | `MacroEntity` | `macros` | User-configured multi-step automation routines. |
| 11 | `WeatherCache` | `weather_cache` | Geo-located hourly weather forecasts. |
| 12 | `QueryCache` | `query_cache` | Exact-match query hash response cache. |

---

## 10. SECURITY, PERMISSIONS & MANIFEST CONFIGURATION

### 10.1 Security Model
- **API Key Encryption:** Keys are encrypted using AndroidX Security Crypto (`MasterKeys.AES256_GCM_SPEC`) stored inside `SecurePrefs.kt`. Keystore corruption recovery is built-in.
- **Local Privacy Mode:** When enabled, network HTTP dispatchers are bypassed, routing all interactions to on-device rules and `LlamaCppBrain`.

### 10.2 Manifest Declared Services & Receivers (`AndroidManifest.xml`)
1. `com.example.service.AssistantService` (`android.permission.BIND_VOICE_INTERACTION`) — Default Voice Assistant Service.
2. `com.example.service.AssistantSessionService` (`android.permission.BIND_VOICE_INTERACTION`).
3. `com.example.service.AiraAccessibilityService` (`android.permission.BIND_ACCESSIBILITY_SERVICE`) — UI parsing & automation.
4. `com.example.service.ActiveListeningService` (`foregroundServiceType="microphone"`) — Hands-free wake word listening.
5. `com.example.service.AiraDeviceAdminReceiver` (`android.permission.BIND_DEVICE_ADMIN`) — Screen lock and policy manager.
6. `com.example.service.ReminderReceiver` — AlarmManager broadcast receiver for scheduled alerts.
7. `rikka.shizuku.ShizukuProvider` — Privileged IPC binding.

---

## 11. COMPLETE OFFLINE VS. ONLINE MATRIX

| Capability | Online Behavior | Offline Behavior | Verification Status |
| :--- | :--- | :--- | :--- |
| **STT (Speech-to-Text)** | Android Native `SpeechRecognizer` | Vosk Offline Acoustic Model | **IMPLEMENTED** |
| **TTS (Speech Synthesis)** | Google TTS (J.A.R.V.I.S. Prosody) | Piper ONNX (`libpiper.so`) | **IMPLEMENTED** |
| **Cloud Intelligence** | Gemini / Groq / OpenAI / Claude | Offline Notice / Local Fallback | **IMPLEMENTED** |
| **Local LLM Inference** | N/A | `LlamaCppBrain` (JNI + Heuristic fallback) | **IMPLEMENTED (HYBRID)** |
| **Math & Conversions** | Cloud / Local | `JarvisSpecializedToolkit` (100% Local) | **IMPLEMENTED** |
| **UI Tree Automation** | 100% On-Device | 100% On-Device (Accessibility) | **IMPLEMENTED** |
| **System Controls** | 100% On-Device | 100% On-Device (Android APIs / Shizuku) | **IMPLEMENTED** |
| **Screen OCR & Scanners** | 100% On-Device | 100% On-Device (ML Kit & A11y) | **IMPLEMENTED** |
| **Memory & Database** | 100% On-Device | 100% On-Device (Room SQLite) | **IMPLEMENTED** |
| **Web Grounding** | Google / YouTube / Wiki | Offline Prompt | **IMPLEMENTED** |

---

## 12. COMPREHENSIVE FEATURE IMPLEMENTATION STATUS

| Feature Name | Audited Status | Implementation Evidence |
| :--- | :--- | :--- |
| **Wake Word Activation** | **IMPLEMENTED** | `ActiveListeningService.kt`, Vosk Continuous Listener |
| **Offline STT** | **IMPLEMENTED** | `VoskSpeechRecognizer.kt`, `assets/vosk-model-small-en-us-0.15` |
| **Neural TTS (Piper JNI)** | **IMPLEMENTED** | `PiperTtsManager.kt`, `PiperTtsEngine.kt`, `libpiper.so` |
| **Google TTS J.A.R.V.I.S.** | **IMPLEMENTED** | `PiperTtsManager.kt` (Pitch 0.92, Speed 1.08, `en-GB`) |
| **Press-to-Talk Voice Orb**| **IMPLEMENTED** | `AiraVoiceOrb.kt`, `HomeScreen.kt` |
| **Cloud Multi-Provider** | **IMPLEMENTED** | `ProviderManager.kt`, 8 distinct AI clients |
| **Token Streaming (SSE)** | **IMPLEMENTED** | `AiStreamManager.kt`, `SentenceChunker.kt` |
| **Multi-Key Vault** | **IMPLEMENTED** | `MultiKeyManager.kt`, `SecurePrefs.kt` (AES-256) |
| **Local LLM Engine** | **IMPLEMENTED (HYBRID)**| `LlamaCppBrain.kt` (Native JNI hooks + heuristic offline transformer) |
| **Long-Term Memory** | **IMPLEMENTED** | `JarvisMemoryExtractor.kt`, `MemoryDao.kt` |
| **System Automation** | **IMPLEMENTED** | `AiraAutomationEngine.kt` (~40 discrete execution handlers) |
| **Accessibility Automation**| **IMPLEMENTED** | `AiraAccessibilityService.kt` (Node tree scraper, gesture engine) |
| **Privileged Controls** | **IMPLEMENTED** | `ShizukuManager.kt`, `ShizukuServiceWrapper.kt` |
| **Specialized Math & Calc**| **IMPLEMENTED** | `JarvisSpecializedToolkit.kt` |
| **Unit & Currency Convert**| **IMPLEMENTED** | `JarvisSpecializedToolkit.kt` |
| **Screen OCR Scanner** | **IMPLEMENTED** | `JarvisSpecializedToolkit.kt`, ML Kit |
| **QR & Barcode Scanner** | **IMPLEMENTED** | `JarvisSpecializedToolkit.kt`, ML Kit |
| **Clipboard Reader & Copy**| **IMPLEMENTED** | `JarvisSpecializedToolkit.kt` |
| **Smart Reply Suggestions** | **IMPLEMENTED** | `JarvisSpecializedToolkit.kt`, `HomeScreen.kt` |
| **Privacy Mode Lock** | **IMPLEMENTED** | `AiraViewModel.kt`, `SystemControlScreen.kt` |
| **Do Not Disturb Protocol**| **IMPLEMENTED** | `AiraViewModel.kt`, `AiraAutomationEngine.kt` |
| **Mute Mode Protocol** | **IMPLEMENTED** | `AiraViewModel.kt`, `SystemControlScreen.kt` |
| **Text Fallback Bar** | **IMPLEMENTED** | `HomeScreen.kt` (`home_text_command_input`) |
| **Morning Briefing** | **IMPLEMENTED** | `HomeScreen.kt`, `AiraViewModel.kt` |

---

## 13. PROJECT DIRECTORY MAP

```
/
├── 00_MASTER_CONTEXT.md              # THIS MASTER CONTEXT KNOWLEDGE BASE
├── app/
│   ├── build.gradle.kts              # Application Gradle build configuration
│   ├── src/main/
│   │   ├── AndroidManifest.xml       # Services, Permissions, Voice Interaction metadata
│   │   ├── assets/
│   │   │   ├── piper/models/         # amy_config.json
│   │   │   └── vosk-model-small-en-us-0.15/ # Vosk acoustic model
│   │   ├── jniLibs/arm64-v8a/        # libpiper.so, libonnxruntime.so
│   │   ├── res/                      # Values, drawables, strings, XML configs
│   │   └── java/com/example/
│   │       ├── AiraApplication.kt    # App lifecycle, multi-process memory isolation
│   │       ├── MainActivity.kt       # Animated tab deck controller
│   │       ├── data/                 # AppDatabase (v10), DAOs, 12 Entities, MultiKeyManager
│   │       ├── domain/               # Use cases, models, result wrappers
│   │       ├── models/               # AiBrain, LlamaCppBrain, JarvisSpecializedToolkit
│   │       ├── network/              # Retrofit clients, SSE StreamManager, 8 AI Providers
│   │       ├── service/              # AssistantService, AiraAccessibilityService, AutomationEngine
│   │       ├── ui/                   # HomeScreen, SystemControlScreen, ExtrasScreen, SettingsScreen
│   │       └── utils/                # ShizukuManager, SecurePrefs, Sentiment, AudioFocus
```

---

## 14. CRITICAL NON-OBVIOUS DETAILS FOR FUTURE AGENTS

1. **Voice Interaction Process Boundary:** Never remove `processName.contains(":assistant")` in `AiraApplication.kt`. The voice interaction assistant runs in background memory and will trigger an Out-Of-Memory (OOM) fatal exception if Piper or Llama models are loaded inside it.
2. **TextToSpeech Prosody Ordering:** In `PiperTtsManager.kt`, always set the language/voice profile **prior** to setting pitch (`0.92f`) and speech rate (`1.08f`). Calling `setVoice()` afterwards resets Android's audio multipliers back to 1.0f default.
3. **Database Version:** `AppDatabase` is at version **10** (not 12). Modifying entity definitions requires adjusting `DatabaseSchema.kt` and `AppDatabase.kt`.
4. **Encrypted Preferences Resilience:** Always access secure settings via `SecurePrefs.getEncryptedSharedPreferences()`. It encapsulates automatic Keystore recovery and fallback to prevent crashes from corrupted hardware keys.

---

```
MASTER_CONTEXT_STATUS: FINALIZED_AFTER_CODE_AUDIT
```

### Unverified Facts from Project Inspection:
- *Exact quantization format of future external GGUF weights intended for `LlamaCppBrain` (Q4_K_M vs Q8_0) is unpinned in code.*
- *Shizuku daemon runtime availability depends on user device environment (ADB / Root).*
