# EVA — Evolutionary Voice Agent (Phase 1: Foundation)

Package: `com.eva.evolutionaryai`

## What this build actually is

This is Phase 1 only, exactly as scoped: a real Android app (Kotlin + Jetpack
Compose) with speech recognition, text-to-speech, a provider-independent AI
interface, an offline rule-based fallback, encrypted settings storage for API
keys, and honest IDLE/LISTENING/THINKING/SPEAKING/ERROR status reporting.

No action system, memory persistence, code agent, build engine, guardian, or
evolution engine is implemented yet — those are Phases 2–10. Where the spec's
example commands ("Open YouTube", "Upgrade yourself") aren't backed by real
functionality yet, EVA says so explicitly instead of pretending.

## Important: I could not run the build in the sandbox that wrote this code

The environment I used to generate these files has no Android SDK, no Gradle
installation, and **no network access at all** (outbound requests are
blocked). That means I could not run `./gradlew assembleDebug` and cannot
hand you a verified APK from this session — and per your own instructions, I
won't claim a build succeeded when I never actually ran it.

This isn't a gap in the plan, though — it matches what you described: EVA is
meant to be built and maintained from an Android device using AndroidIDE/ADE,
which does have the SDK, a JDK, and network access. Two things to do there:

1. **Generate `gradle/wrapper/gradle-wrapper.jar`.** I wrote
   `gradlew`, `gradlew.bat`, and `gradle-wrapper.properties`, but the wrapper
   `.jar` itself is a binary Gradle distributes over the network — I can't
   fabricate those bytes offline. On a machine/device with Gradle available,
   run once from the project root:
   ```
   gradle wrapper --gradle-version 8.7
   ```
   AndroidIDE typically generates this automatically on project import/sync;
   if so you can skip this step entirely.

2. **Then build for real:**
   ```
   ./gradlew assembleDebug
   ```
   Output APK: `app/build/outputs/apk/debug/app-debug.apk`

If the build fails on your device (missing SDK components, AGP/Kotlin version
mismatch for your installed SDK, etc.), that's real compiler/linker output —
read it and fix it there. I used known-stable, compatible versions (AGP
8.5.2, Kotlin 1.9.24, Compose BOM 2024.06.00, compileSdk/targetSdk 34,
minSdk 26, JDK 17 target) but I have not executed this exact combination in
this session, so treat it as "should build," not "confirmed builds."

## Architecture notes for Phase 2+

- `ai/AIProvider.kt` — the interface every future provider (local model,
  another vendor, etc.) implements. `EvaBrain` only talks to this interface,
  never to a concrete provider, so swapping/adding providers won't require
  touching the brain or UI.
- `settings/SettingsManager.kt` — all provider config, including API keys, is
  stored via `EncryptedSharedPreferences` (Android Keystore-backed AES-256).
  Nothing is hard-coded, nothing is logged.
- `EvaStatus` already includes `WORKING`, unused in Phase 1, reserved for the
  Build/Guardian/Evolution engines so their status reporting slots in without
  changing every call site that switches on status.
- Voice: Android's `SpeechRecognizer` only supports foreground, permission-
  gated listening sessions — there is no real continuous background listening
  API to wrap, so none is claimed here.

## What's NOT in this build (by design, per spec section 20)

Action system, persistent memory, code agent, build engine, Guardian repair
loop, Evolution Engine, automation/scheduling, update system. Adding these is
Phases 2–10.
