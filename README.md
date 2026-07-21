# Notification Forwarder

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

Everything is done **inside the app** — a built-in step-by-step wizard walks
you through it, with buttons that open the right Telegram bots for you. No
technical knowledge required.

### 1. Install the app

Grab the APK from the [Releases](../../releases) page (or build from source,
see below) and sideload it:

```bash
adb install NotificationForwarder.apk
```

You may need to enable "Install unknown apps" for your file manager/browser.

### 2. Follow the in-app wizard

Open the app and work down the numbered cards:

1. **Create your bot** — tap **Open BotFather**, send `/newbot`, and paste the
   token it gives you back into the app.
2. **Get your Chat ID** — tap **Get my Chat ID** (opens `@userinfobot`), press
   START, and paste the number it replies with.
3. **Save Settings.**
4. **Allow access** — grant SMS/call permissions and enable Notification Access.
5. **Start forwarding** and tap **Send Test Message** to confirm it works.

Each user configures their **own** bot, so messages only ever go to a Telegram
chat you control.

## Building from source

Requirements:

- JDK 17 (`JAVA_HOME` set, or `javac` on your `PATH`)
- Android SDK with **build-tools 33.0.2** and **platform android-33**
  (set `ANDROID_HOME`, or the script checks common install locations)

```bash
./build.sh
```

The script generates a throwaway `debug.keystore` on first run and produces
`NotificationForwarder.apk`. This debug key is fine for personal sideloading but **not**
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
