# QuestBiliFix

A small LSPosed / legacy-Xposed module to fix a **Bilibili HD** crash on **Meta Quest 3 (Horizon OS)**.

## What crashed?

Observed on Quest 3 / Android 14 / Bilibili HD 2.0.1 (version code 2001100), package `tv.danmaku.bilibilihd`:

```text
FATAL EXCEPTION: thread_back_io
com.alibaba.fastjson.JSONException: syntax error, pos 180
    at com.alibaba.fastjson.JSON.parseArray(...)
    at com.bilibili.lib.biliid.internal.fingerprint.Fingerprint.n(...)
```

The application inserts a Quest-specific sensor name, beginning with `{"aname":"Syncboss IMU Android Sensor",...`, directly into an outer JSON string without escaping its quotes and newlines. The malformed document then crashes the device fingerprint collector.

**Do not publicly upload the complete original crash log:** that sensor name also contains potentially private device calibration and serial data.

## How it works

- Only executes in the `tv.danmaku.bilibilihd` package; LSPosed also restricts the module's recommended scope to that package.
- Hooks `android.hardware.Sensor.getName()`, returning the short name `Syncboss IMU Android Sensor` **only** for a type `65537` sensor whose original name is the known Quest calibration JSON.
- Independently hooks `com.alibaba.fastjson.JSON.parseArray(...)` in the app's class loader. Only the known malformed Quest sensor record is corrected. All other JSON parsing remains unchanged.
- Does not touch measurements, IMU calibration, the real sensor service or any other application.
- Does not upload anything. Logs only short status messages such as `[QuestBiliFix] repaired malformed Quest sensor JSON`.

The fallback algorithm was tested against the supplied crash payload: the corrected 14.5 KB JSON string parses into exactly three sensors. No private calibration data is checked into this repository.

## Install on Quest

1. Open [Actions → Build APK](https://github.com/grayawa/QuestBiliFix/actions/workflows/build.yml).
2. Open a successful workflow run. Download and extract the **QuestBiliFix-debug-apk** artifact.
3. Install `app-debug.apk` on the headset, e.g. `adb install app-debug.apk`.
4. In LSPosed, enable **QuestBiliFix**, scope it to **哔哩哔哩 HD** (`tv.danmaku.bilibilihd`) only.
5. Force-stop Bilibili HD, then start it again (reboot if LSPosed asks).
6. Inspect the LSPosed logs for `[QuestBiliFix]` to confirm the module loaded and whether the sensor-name or JSON fallback hook fired.

**No launcher icon:** this is normal for an Xposed module. The APK is only a module, not a standalone player.

**Signing note:** each GitHub Actions runner may generate a different debug signing key. If installing a newer CI APK fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, uninstall only the previous *QuestBiliFix* module APK first, then install the replacement and re-enable it in LSPosed. Do not uninstall Bilibili HD.

## Build locally

JDK 17, Android SDK (API 35), Gradle 8.9:

```sh
gradle :app:testDebugUnitTest :app:assembleDebug
```

Result: `app/build/outputs/apk/debug/app-debug.apk`.

The `xposed-api-stubs` Java project is **compileOnly**, and the Xposed classes are not included in the Android APK. The real implementation is supplied by LSPosed. This avoids reliance on old, defunct Xposed/JCenter Maven repositories.

## Limitations

- Built for the Quest-specific JSON failure seen in Bilibili HD 2.0.1. Not yet tested on a physical headset.
- Requires LSPosed with legacy Xposed API compatibility.
- If an app update changes its JSON parser or fingerprint collection, this fix may need an update.

## License

MIT — see [LICENSE](LICENSE).
