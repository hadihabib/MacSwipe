# MacSwipe — GitHub Edition v0.2

For an Android APK without Android Studio, see **GITHUB-START-AR.md**.
The repository includes **.github/workflows/build-android-apk.yml** and builds a debug APK via GitHub Actions.

---

# MacSwipe — Android phone as a Mac trackpad (MVP v0.1)

An original Android app with a small macOS Swift receiver. Uses local Wi-Fi with token pairing. No cloud, accounts, or subscription.

## What works

| On Android | On Mac |
|---|---|
| One finger move | Move mouse pointer |
| One finger tap | Left click |
| Long press then move | Drag windows/files |
| Two fingers swipe | Vertical/horizontal scrolling |
| Two fingers tap | Right click |
| Three fingers swipe left/right | Switch between desktop Spaces (Control + left/right arrow) |
| Three fingers swipe up | Mission Control (Control + up arrow) |
| Three fingers swipe down | App Exposé (Control + down arrow) |
| Top shortcut buttons | Spaces, Mission Control, Copy, Paste, Esc |

**Important:** Desktop swipes are implemented using the equivalent *macOS keyboard shortcuts*, not actual native Magic Trackpad multi-touch events. Keyboard shortcuts must be enabled in System Settings if modified.

## Mac setup

1. Open the `mac` folder on your Mac.
2. Double-click `start.command`. On the first run, macOS may block an unidentified script: right-click → Open, or run `cd <mac_folder> && ./start.command` in Terminal.
3. If the Swift compiler is missing, run `xcode-select --install` in Terminal and rerun.
4. In **System Settings → Privacy & Security → Accessibility**, allow your Terminal (or the compiled receiver) to control your Mac, then restart `start.command`.
5. Leave Terminal running. Note the **IP** and **Token** printed in its window. Mac may ask permission for incoming local-network connections — allow on your trusted network.

A random 40-character pairing token is generated locally in `mac/.macswipe_token` with restrictive file permissions. Keep it private.

## Android setup

1. Open `android/` as a project in **Android Studio** (JDK 17, Android SDK 35).
2. Allow Gradle sync, then Run the app on your Android phone. Or use **Build → Build APK(s)** and install the created APK on your phone.
3. Make sure phone and Mac use the same **trusted Wi-Fi network**. On the phone enter Mac IP and pairing token from Terminal, and tap Connect.
4. Try cursor movement and swipes on the large touchpad area.

The project includes Gradle config. If Android Studio says Gradle wrapper is missing, use Android Studio's project import to select Gradle 8.9, or generate its wrapper with `gradle wrapper --gradle-version 8.9`. An APK is *not* included in this source archive.

## Troubleshooting

- **Connection refused/time-out:** Both devices on the same LAN; check guest Wi-Fi isolation, VPN, IP address, and macOS Firewall local connections. The service binds **only to the selected Mac Wi-Fi IPv4** on TCP port 8765.
- **Connects but pointer won't move:** Grant Accessibility to the executable/Terminal actually running the receiver. Stop and restart it afterward.
- **Three-finger swipe does nothing:** In macOS, check System Settings → Keyboard → Keyboard Shortcuts → Mission Control, ensure Control + Arrow Space shortcuts are configured. At least two desktop Spaces must exist to switch.
- **Swipe direction doesn't feel natural:** Reverse `space_left`/`space_right` mapping in `TrackpadView.kt`; change scroll sign in `MacReceiver.swift` if preferred.
- **Source code check:** `swiftc -frontend -parse mac/MacReceiver.swift` checks Swift syntax; a full macOS compilation and actual Android build require a Mac / Android SDK respectively.

## Security and limitations

- Receives only predefined JSON commands. There is no arbitrary code/shell execution.
- Random token is needed for connection; the receiver binds only to local Wi-Fi IP.
- **TCP is not encrypted** — use only on private, trusted Wi-Fi. Do NOT expose port 8765 to the internet or share the token. Production use across untrusted networks would require TLS/pairing improvements.
- This is an **untested MVP source**. We have not compiled it on an actual Mac or built/installed an APK in Android Studio yet.
- No screen mirroring or Mac screen preview; this is a remote touchpad/controller.
