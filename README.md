<h1 align="center">Scrolless with Friends</h1>
<h3 align="center">Anti Brain Rot App — now with friends who ration your brain rot</h3>

<p align="center"><img src="art/app_logo.png" alt="Scrolless" height="256"></p>
<p align="center">
  <br/>
  <a href="https://opensource.org/license/gpl-3"><img src="https://img.shields.io/badge/License-GPL_3.0-blue.svg?color=3F51B5&style=for-the-badge&label=License&logoColor=000000&labelColor=ececec" alt="License: GPLv3"></a>
  <br/>
  <br/>
</p>

> [!NOTE]
> **This is a vibecoded fork** of [Scrolless](https://github.com/duartebarbosadev/Scrolless) by Duarte Barbosa — built by iterating with [Claude Code](https://claude.com/claude-code) as the pair programmer. Design decisions, reviews, tests, and most of the code in the new features came out of that loop. All the original blocking goodness is upstream's work; the social layer below is what this fork adds.

## What we are building

The original Scrolless blocks Reels/Shorts/TikTok on your own willpower's terms. This fork adds a **social layer**: your daily scroll budget is small, and only *other people* can top it up.

- **Partner Quota mode** — a fourth blocking mode: **15 minutes of short-form content per window** (morning 05–12, afternoon 12–18, night 18–05). When a window's quota is gone, content is blocked until the next window.
- **Gifts of time** — want more than 15 minutes? A friend with Scrolless can send you a **+15 minute gift**: they generate a gift link in their app (Settings → *Send a 15-minute gift*) and send it over WhatsApp or any chat. Tapping the link opens Scrolless and redeems it with a little celebration; pasting the message works too.
- **Strict mode** — an opt-in time lock (1 hour to 30 days, extendable but never shortenable). While it is armed, Scrolless closes any system screen that could switch it off: its accessibility toggle, its app-info page, force-stop and uninstall dialogs, Settings search results and its Play Store page. The countdown runs on elapsed-realtime, so changing the device clock does nothing. Safe mode and ADB remain as deliberate escape hatches.
- **Minimal mode** — the inverse of blocking a list: during hours you choose, *everything* closes itself except an allowlist you pick. Calls, the emergency dialer, alarms, the home screen, the keyboard and the system UI are always allowed and cannot be removed. The schedule reads a clock derived from elapsed-realtime, so winding the device clock does not move your hours. While strict mode is armed the switch can only go on, apps can only be removed from the allowlist, and the hours can only grow.
- **What minimal mode does not stop** — it closes an app about a quarter-second *after* it opens, so you see it briefly; Recents and the notification shade still show content; widgets and the lock screen are not apps and are not guarded; and any app you add to the allowlist is fully open, WhatsApp included, Status and Channels and all. It is a deterrent with real friction, not a kiosk — a Device Owner setup over ADB is the only way to get the system itself to enforce this.
- **Cheat resistance where it counts** — quota windows survive clock changes (wall clock is cross-checked against elapsed-realtime and boot count; suspicious jumps never refill quota), gift codes are single-use with a 24-hour lifetime, and backup-restore tricks are closed off.
- **Honest about limits** — gift codes are validated with a key that ships in this open-source app, so they prove a code is well-formed, not who made it. This is deliberate: the feature is a *social ritual with friction*, not DRM. If you want to cheat yourself, no app can stop you.
- **Still zero permissions, still fully offline** — the app declares no permissions beyond the accessibility service and even strips the INTERNET permission at build time via the manifest merger. Gift links travel through *your* messaging apps; Scrolless itself never touches the network.

Roadmap ideas: clock-tamper hardening for the original Daily Limit / Interval modes, upstream bug fixes, and whatever the next iteration session comes up with.

## Original features (from upstream Scrolless)

**Scrolless** is an open-source Android application designed to help users avoid excessive consumption of brain rot by blocking distracting content like Instagram Reels, TikTok, and YouTube Shorts.

Using Android's accessibility permissions, the app detects and blocks this type of content whenever it appears on the screen.
Since the app requires accessibility permissions, which can have sketchy uses, Scrolless is fully open-source.

- **Block All**
  Instantly block access to all supported platforms, including Instagram Reels, TikTok, YouTube Shorts, and Facebook Reels.

- **Daily Limit**
  Set a daily limit for how long you can spend on supported platforms. Once the limit is reached, access is blocked for the rest of the day.

- **Live Brain Rot Timer**
  A real-time overlay timer tracks your session while watching Shorts or Reels, keeping you aware of your screen time.

- **Pause**
  Pause the blocking feature for 5 minutes, allowing temporary access to the platforms.

- **Interval Timer**
  Set intervals for usage and breaks, allowing controlled social media access throughout the day.

- **Usage Tracking**
  Keep track of how much time you’ve spent on Instagram Reels, TikTok, YouTube Shorts, and Facebook Reels.

## Screenshots

<img src="art/Scrolless.png" alt="Scrolless app" width="250">

# Architecture

Scrolless app architecture is inspired by the Google open source project [Jetcaster](https://github.com/android/compose-samples/tree/main/Jetcaster), published under the [Apache License](https://github.com/android/compose-samples/blob/main/LICENSE).

Some icons used in this app are obtained from [Icons8](https://icons8.com).

## Credits

- Original app: [duartebarbosadev/Scrolless](https://github.com/duartebarbosadev/Scrolless) (GPL-3.0) — go star it and get it on [Google Play](https://play.google.com/store/apps/details?id=com.scrolless.app).
- This fork's social features were developed together with Claude Code.
