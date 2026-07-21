# SMS Forwarder

A small, self-hosted Android app that forwards **your own** phone's incoming
SMS messages, call events, and app notifications to a Telegram chat that
**you** control.

It's meant as a personal tool — for example, to read messages from a spare/backup
phone, a travel SIM, or an old device you keep at home, without having to pick it up.

---

## ⚠️ Important — read before installing

This app can access sensitive personal data: **incoming SMS, call logs, and
every notification** shown on the device. Because of that:

- **Only install this on a phone you own and control.** Installing an app that
  silently forwards messages, calls, and notifications onto **someone else's**
  phone without their knowledge and consent is **stalkerware** — it is unethical
  and illegal in most jurisdictions.
- Each user configures **their own** Telegram bot token and chat ID inside the
  app. The project ships with **no credentials** — nothing is sent anywhere
  until you enter your own details.
- Messages are sent to Telegram's servers over HTTPS. Review Telegram's privacy
  policy and decide whether that's acceptable for your data.

The authors provide this software "as is", with no warranty, and accept no
liability for misuse. See [LICENSE](LICENSE).

---

## Features

- Forwards incoming **SMS** to Telegram
- Forwards **incoming / missed call** events
- Forwards **notifications** from other apps (with de-duplication)
- Runs as a foreground service and restarts after reboot
- Per-user configuration via an in-app settings screen (no code editing needed)

## Setup

### 1. Create a Telegram bot

1. Open Telegram and message [@BotFather](https://t.me/BotFather).
2. Send `/newbot` and follow the prompts to get a **bot token**
   (looks like `123456789:ABCdef...`).
3. Send any message to your new bot so it can message you back.
4. Get your **chat ID**: message [@userinfobot](https://t.me/userinfobot), or
   open `https://api.telegram.org/bot<YOUR_TOKEN>/getUpdates` in a browser after
   messaging your bot and read the `chat.id` value.

### 2. Install the app

Build from source (see below) or grab a release APK, then sideload it:

```bash
adb install SmsForwarder.apk
```

You may need to enable "Install unknown apps" for your file manager/browser.

### 3. Configure and grant access

Open the app, then:

1. Enter your **bot token** and **chat ID**, and tap **Save Telegram Settings**.
2. Tap **Grant SMS & Call Permissions** and allow them.
3. Tap **Enable Notification Access** and toggle the app on in the system screen.
4. Tap **Start Forwarder Service**.
5. Tap **Send Test Message to Telegram** to confirm it works.

## Building from source

Requirements:

- JDK 17 (`JAVA_HOME` set, or `javac` on your `PATH`)
- Android SDK with **build-tools 33.0.2** and **platform android-33**
  (set `ANDROID_HOME`, or the script checks common install locations)

```bash
./build.sh
```

The script generates a throwaway `debug.keystore` on first run and produces
`SmsForwarder.apk`. This debug key is fine for personal sideloading but **not**
for publishing to an app store — generate your own release key for that.

## How it works

| Component | Role |
|-----------|------|
| `SmsReceiver` | Catches incoming SMS broadcasts |
| `CallReceiver` | Detects incoming / missed calls |
| `NotificationListener` | Reads notifications from other apps |
| `ForwarderService` | Foreground service that keeps forwarding alive |
| `BootReceiver` | Restarts the service after reboot |
| `TelegramSender` | Sends messages to your Telegram chat |
| `MainActivity` | Settings + permissions UI |

Your token and chat ID are stored locally in the app's private
`SharedPreferences` and never leave the device except when contacting the
Telegram Bot API.

## License

[MIT](LICENSE)
