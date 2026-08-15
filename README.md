# Aira Voice Assistant

![Build Android APK](https://github.com/USER/REPO/actions/workflows/build.yml/badge.svg)

Aira is an intelligent responsive vocal assistant featuring an active Iron Man HUD interface, customizable styling, offline/online speech capability, and an integrated AI brain powered by Gemini and Groq.

## Release APK Secrets Guide
To generate signed release APKs automatically using GitHub Actions, configure the following secrets in your GitHub Repository under **Settings > Secrets and variables > Actions**:

1. `KEYSTORE_BASE64`: Base64 encoded string of your `.jks` or `.keystore` file (`base64 -w 0 release.keystore`).
2. `KEYSTORE_PASSWORD`: Password for your keystore.
3. `KEY_ALIAS`: Alias for your signing key.
4. `KEY_PASSWORD`: Password for your signing key.

## Features
- **Vocal AI Brain**: Fluid conversational capabilities utilizing Groq (Llama) and Google Gemini with persistent offline fallback caching.
- **Offline Text-To-Speech (TTS)**: Incorporates localized speech engines (Piper/Vosk) to ensure continuous speech response availability.
- **System Control Panel**: Direct hardware controls including volume levels, display brightness, and Bluetooth connectivity toggles.
- **Dynamic Themes**: Fluid transition between light and dark visual themes with custom accent colors.
- **Iron Man HUD UI**: Immersive, futuristic, active styling layout modeled on sleek cybernetic head-up displays.
- **Robust Global Error Handling**: Integrated error monitoring system that triggers immediate, user-friendly snackbar updates.

## Technical Stack
- **UI Framework**: Jetpack Compose (Kotlin) with full Material 3 compliance.
- **Architecture**: MVVM (Model-View-ViewModel).
- **Local Database**: Room DB for local caching and history.
- **Networking**: OkHttp Client with custom interceptors and error wrapper logic.
- **Core Speech APIs**: Native Android SpeechRecognizer and TextToSpeech.

## How to Run
1. Open this project in **Android Studio**.
2. Sync the project with Gradle files.
3. Add Gemini/Groq API keys in the environment or runtime configurations if needed.
4. Build and run the app on an Android Emulator or physical device.
