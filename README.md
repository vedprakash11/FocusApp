# Focus Timer (Pomodoro)

Android Focus Timer app in Kotlin. Pomodoro-style phases: Focus, Short break, Long break. Optional Google Ads (banner); requires internet when ads are used.

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
- **INTERNET** – used only for Google Ads (banner)

No location or storage permissions. No user data collection beyond what AdMob uses for ad serving.

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

Release: `./gradlew assembleRelease` (requires signing config below).

---

## App Signing (very important for Play Store)

### 1. Create a keystore (once; save it forever)

- **Windows:** run `create_keystore.bat`
- **macOS/Linux:** run `./create_keystore.sh` (or `sh create_keystore.sh`)

You will be asked for:

- **Keystore password** – choose a strong password and store it safely.
- **Key password** – can be the same as the keystore password.

This creates `focus-timer-release.jks`. **Back up this file and both passwords permanently.** You need them for every future app update on Play Store. If you lose them, you cannot update the same app listing with a new key.

### 2. Configure signing in the project

1. Copy the example config:
   ```bash
   copy keystore.properties.example keystore.properties
   ```
2. Edit `keystore.properties` (do **not** commit it):
   - `storePassword` = your keystore password  
   - `keyPassword` = your key password  
   - `keyAlias` = `focus-timer`  
   - `storeFile` = path to your `.jks` file (e.g. `../focus-timer-release.jks` or absolute path)

3. Build a signed release:
   ```bash
   ./gradlew assembleRelease
   ```
   The signed APK is in `app/build/outputs/apk/release/`.

### 3. Play Store

- **Manage signing:** You can upload your first AAB/APK signed with the keystore above, or use **Play App Signing** and let Google manage the app signing key (you keep and use the upload key from this keystore).
- **Optimize APK splits:** Play Store will generate optimized APKs/splits per device; you only need to upload the release AAB (e.g. from `./gradlew bundleRelease`).

**Security:** Never commit `keystore.properties` or `*.jks`/`*.keystore` to version control. They are listed in `.gitignore`.

---

## Google Ads (AdMob)

The app shows a **banner ad** at the bottom of the screen. Test ad unit IDs are configured by default so the app runs without an AdMob account.

### Use your own ad units (production)

1. Create an [AdMob](https://admob.google.com/) account and add your app.
2. Create an **App** in AdMob and note the **App ID** (e.g. `ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY`).
3. Create a **Banner** ad unit for that app and note the **Ad unit ID** (e.g. `ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ`).
4. In the project:
   - **AndroidManifest.xml** – replace the `com.google.android.gms.ads.APPLICATION_ID` meta-data value with your **App ID**.
   - **res/values/strings.xml** – set `ad_unit_id_banner` to your **Banner ad unit ID**.
5. Rebuild and test with real ads (follow [AdMob policy](https://support.google.com/admob/answer/6128543)).

### Disable ads

To remove ads: delete the `AdBanner` usage from `FocusAppNav.kt`, remove the `play-services-ads` dependency and `MobileAds.initialize()` from `MainActivity`, and remove the `INTERNET` permission and AdMob `APPLICATION_ID` meta-data if the app no longer needs network access.
