# DarQ Reborn <a href="https://github.com/Arora-Sir/DarQ-Reborn/releases"><img src="https://img.shields.io/github/downloads/Arora-Sir/DarQ-Reborn/total?style=for-the-badge&color=bf7830&labelColor=805020" align="center" alt="Downloads"></a> <a href="https://github.com/Arora-Sir/DarQ-Reborn/releases/latest"><img src="https://img.shields.io/github/v/release/Arora-Sir/DarQ-Reborn?style=for-the-badge&color=3060bf&labelColor=204080&label=Latest" align="center" alt="Latest Release"></a>


![DarQ](assets/darq_banner.png)

DarQ provides a per-app selectable force dark option for Android 10 and above.

> [!NOTE]
> This is a modded fork maintained by [Mohit Arora](https://github.com/Arora-Sir). The original repository by [KieronQuinn](https://github.com/KieronQuinn/DarQ) is archived.

> [!IMPORTANT]
> **Upgrading from the Official Release (v2.2.1 or older):** Due to a signature mismatch from the transition to this fork, you **must uninstall the official version first** before installing updates from this repository. Future updates from this fork will upgrade directly as normal.

It uses a root or [Shizuku](https://shizuku.rikka.app/) (ADB) service to apply the theme seamlessly and quickly, without needing an accessibility service.

## Requirements & Setup

DarQ requires either **Root Access** or the **Shizuku** service to be running on your device to modify system theme properties.

### Shizuku Setup (For Non-Rooted Devices)
If your device is not rooted, you must set up **Shizuku** before running DarQ:
1. **Download Shizuku:**
   * **Recommended (Modded Fork):** Download and install the [thedjchi Shizuku Fork](https://github.com/thedjchi/Shizuku/releases). This version is actively maintained and highly recommended for custom, aggressive OEM skins (such as Xiaomi/HyperOS, OPPO/ColorOS, etc.) because it includes:
     * A **Watchdog service** that automatically restarts Shizuku's background process if it gets killed by the system.
     * Robust boot startup logic (e.g., waiting for Wi-Fi connection).
     * Quality-of-life patches like ADB-over-TCP settings and intent controls.
   * **Original Version:** Alternatively, you can install the original version from the [Google Play Store](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api) or the [RikkaApps Shizuku GitHub Repository](https://github.com/RikkaApps/Shizuku).
2. Open Shizuku and follow the in-app guide to start the service (using Wireless Debugging on Android 11+ or ADB command line on a computer).
3. Once the Shizuku service is running, open DarQ and grant it Shizuku access when prompted.
> [!IMPORTANT]
> **Device-Specific & Background Requirements:**
> * **Xiaomi / Redmi / POCO:** You must enable **"USB Debugging (Security settings)"** in Developer Options, and set Shizuku's Battery Saver to **"No restrictions"** in system App Info.
> * **OPPO / OnePlus / Realme:** You must enable the **"Disable permission monitoring"** (or **"Disable system optimization"** in newer builds) setting in Developer Options to prevent the OS from blocking the connection.
> * **Background Service Termination (OnePlus / Oppo / Xiaomi):** If you find that you have to manually open DarQ to make apps dark again after a while, the system has killed the background DarQ process. Go to **Advanced Options** in the app, enable **"Keep service running in background"**, and click **"Manage Notification"** to hide or minimize the status bar icon if desired.

### Xposed / LSPosed Mode Setup

If you are using the **Xposed / LSPosed** mode (to override apps that block Force Dark in code), the setup requires two steps:

1. **LSPosed Manager -> DarQ module scope**: Add the apps you want Force Dark to work on. This gives DarQ permission to inject into those app processes.
2. **DarQ app -> App picker**: Select the same apps to enable Force Dark on them, **or** enable **"Always use Force Dark"** in **Advanced Options** to skip this step and automatically apply Force Dark to everything in the LSPosed scope.

> [!IMPORTANT]
> **Do not add System Framework or System UI** to the LSPosed scope. DarQ works inside each individual app process and does not need system-level access. Adding System Framework is unnecessary and may cause system UI glitches or instability.

See the [FAQ](https://github.com/Arora-Sir/DarQ-Reborn/blob/master/app/src/main/assets/faq.md#how-should-i-configure-the-lsposed--xposed-scope-for-darq) for more detail on the LSPosed scope configuration.

### Automation & ADB Triggering (MacroDroid/Tasker)
If you use automation apps (such as MacroDroid, Tasker, or Automate) and want to start DarQ's background service externally, you can send an explicit broadcast:
* **Action**: `com.kieronquinn.app.darq.ACTION_START_SERVICE`
* **Package**: `com.kieronquinn.app.darq`

Alternatively, you can trigger it via ADB shell:

```bash
adb shell am broadcast -a com.kieronquinn.app.darq.ACTION_START_SERVICE -p com.kieronquinn.app.darq
```

DarQ also provides an option to apply the system dark theme (as well as selectable force dark) only after sunset and before sunrise, protecting your eyes when it's most needed. 

Please read the Frequently Asked Questions sections in the app or [here](https://github.com/Arora-Sir/DarQ-Reborn/blob/master/app/src/main/assets/faq.md) for more information and some answers to questions.

[Download from GitHub Releases](https://github.com/Arora-Sir/DarQ-Reborn/releases)