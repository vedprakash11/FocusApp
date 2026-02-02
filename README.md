# Focus Timer (Pomodoro)

Android Focus Timer app in Kotlin. Pomodoro-style phases: Focus, Short break, Long break. Fully offline; no backend, no login.

## Features

- **Timer**: Idle / Focus / Short break / Long break with configurable durations (1–60 / 1–30 / 1–60 min)
- **Persistence**: End-time based; survives app background and process kill (no foreground service)
- **Stats**: Total focus sessions, today’s focus time, current streak (stored locally)
- **Notifications**: Local notifications when focus or break ends (including in background via AlarmManager)
- **DND**: Do Not Disturb on focus start; restored on end/pause/reset (when permission granted)
- **Sound & haptics**: Optional sound and vibration on session end

## Permissions

- **VIBRATE** – vibration on session end
- **POST_NOTIFICATIONS** (Android 13+) – timer/break reminders
- **SCHEDULE_EXACT_ALARM** – fire completion at exact end time when app is in background

No internet, location, or storage permissions. No user data collection.

## Privacy

This app works offline. No personal data is collected or sent. All data stays on your device. Full text is shown in **Settings → Privacy policy**.

## Tech

- Kotlin, Jetpack Compose, Material 3
- Min SDK 24, Target SDK 35
- Single Activity, navigation to Settings and Stats
- SharedPreferences for config, timer state, and stats
- End-time timer (no wake locks or foreground service for countdown)

## Build

```bash
./gradlew assembleDebug
```

Release: `./gradlew assembleRelease`.
