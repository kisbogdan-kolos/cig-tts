# Project Plan

I want to create an app that runs in the backround, listens on a websocket for small text blobs and plays that using the system TTS while lowering/ducking the volume of other apps. Optionally there could be a simple view with a textbox for testing. Keep it simple, no tests are needed.

## Project Brief

# Project Brief: CigTTS

## Features
1. **Background WebSocket Listener:** A foreground service that maintains a persistent WebSocket connection to receive incoming text blobs even when the app is minimized or the screen is off.
2. **Text-to-Speech (TTS) Integration:** Utilizes the native Android Text-to-Speech engine to read the received text messages aloud in real-time.
3. **Audio Focus with Ducking:** Automatically manages system audio focus, ducking (lowering) the volume of background media (like music or podcasts) while a text blob is being spoken, and restoring it immediately after.
4. **Testing Interface:** A simple interactive UI allowing the user to manually enter text into a textbox and play it to verify the TTS output and audio ducking behavior.

## High-Level Tech Stack
* **Language:** Kotlin
* **UI Framework:** Jetpack Compose
* **Navigation & Adaptive Strategy:** Jetpack Navigation 3 (state-driven) combined with the Compose Material Adaptive library for all UI layouts.
* **Concurrency:** Kotlin Coroutines & Flow (for handling WebSocket streams, asynchronous TTS callbacks, and UI state management).
* **Networking:** OkHttp (for robust and lightweight WebSocket client implementation).
* **Core Android APIs:** 
  * `android.speech.tts.TextToSpeech` for voice synthesis.
  * `AudioManager` & `AudioFocusRequest` for audio ducking.
  * `Service` (Foreground Service) for background execution.

## Implementation Steps
**Total Duration:** 7m 12s

### Task_1_Core_TTS_and_UI: Implement TTS manager with Audio Focus (ducking) and create the main Compose UI with a text box for testing TTS.
- **Status:** COMPLETED
- **Updates:** Implemented TtsManager with TextToSpeech and AudioFocus (ducking). Updated MainActivity with a Compose UI containing a text field and a button to test TTS manually. Build verified successfully.
- **Acceptance Criteria:**
  - TTS engine initializes successfully
  - Audio focus ducks background volume during speech
  - Compose UI has a textbox and button for manual TTS testing
- **Duration:** 4m 5s

### Task_2_WebSocket_Foreground_Service: Implement OkHttp WebSocket client, Foreground Service for background execution, and integrate permissions. Connect WebSocket messages to TTS playback.
- **Status:** COMPLETED
- **Updates:** Implemented Foreground Service with OkHttp WebSocket client. Connected WebSocket to TtsManager. Handled all required permissions including POST_NOTIFICATIONS and FOREGROUND_SERVICE_MEDIA_PLAYBACK. Updated UI to start/stop the service. Build succeeded.
- **Acceptance Criteria:**
  - Foreground service runs in background
  - WebSocket client connects and receives text
  - Received text triggers TTS playback
  - Permissions are requested and handled properly
- **Duration:** 2m 49s

### Task_3_Run_and_Verify: Run and Verify: Verify application stability (no crashes), confirm alignment with user requirements, and report critical UI issues.
- **Status:** BLOCKED
- **Updates:** The critic agent attempted to run and verify the application, but it terminated because no physical or emulator device was found. Verification is currently blocked.
- **Acceptance Criteria:**
  - make sure all existing tests pass
  - build pass
  - app does not crash
  - websocket, tts, and ducking functionality verified
- **Duration:** 18s

