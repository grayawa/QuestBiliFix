package dev.gray.questbilifix;

import android.hardware.Sensor;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Xposed entry point. Scoped to Bilibili HD, never hooks system_server.
 */
public final class QuestBiliFix implements IXposedHookLoadPackage {
    private static final String TARGET_PACKAGE = "tv.danmaku.bilibilihd";
    private static final String PREFIX = "[QuestBiliFix] ";
    private static final int QUEST_IMU_TYPE = 65537;
    private static final AtomicBoolean SENSOR_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean JSON_LOGGED = new AtomicBoolean();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        XposedBridge.log(PREFIX + "Loaded in " + TARGET_PACKAGE);
        hookSensorName();
        hookFastjson(lpparam.classLoader);
    }

    private static void hookSensorName() {
        try {
            Set<XC_MethodHook.Unhook> hooks = XposedBridge.hookAllMethods(
                    Sensor.class, "getName", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!(param.thisObject instanceof Sensor)) {
                                return;
                            }
                            Sensor sensor = (Sensor) param.thisObject;
                            if (sensor.getType() != QUEST_IMU_TYPE) {
                                return;
                            }
                            Object value = param.getResult();
                            if (!(value instanceof String)) {
                                return;
                            }
                            String name = (String) value;
                            if (name.startsWith("{")
                                    && name.contains("\"aname\":\"Syncboss IMU Android Sensor\"")) {
                                param.setResult("Syncboss IMU Android Sensor");
                                if (SENSOR_LOGGED.compareAndSet(false, true)) {
                                    XposedBridge.log(PREFIX + "Sanitized Quest IMU sensor name");
                                }
                            }
                        }
                    });
            XposedBridge.log(PREFIX + "Sensor.getName hooked: " + hooks.size());
        } catch (Throwable t) {
            XposedBridge.log(PREFIX + "Sensor hook failed: " + t.getClass().getSimpleName());
        }
    }

    private static void hookFastjson(ClassLoader appClassLoader) {
        try {
            Class<?> jsonClass = XposedHelpers.findClassIfExists(
                    "com.alibaba.fastjson.JSON", appClassLoader);
            if (jsonClass == null) {
                XposedBridge.log(PREFIX + "Fastjson class not found; using sensor hook only");
                return;
            }

            Set<XC_MethodHook.Unhook> hooks = XposedBridge.hookAllMethods(
                    jsonClass, "parseArray", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (param.args == null || param.args.length == 0
                                    || !(param.args[0] instanceof String)) {
                                return;
                            }
                            String original = (String) param.args[0];
                            String repaired = SensorJsonFixer.repair(original);
                            if (repaired != original) {
                                param.args[0] = repaired;
                                if (JSON_LOGGED.compareAndSet(false, true)) {
                                    // Intentionally never log source JSON or serial/calibration info.
                                    XposedBridge.log(PREFIX + "Repaired malformed Quest sensor JSON");
                                }
                            }
                        }
                    });
            XposedBridge.log(PREFIX + "Fastjson parseArray hooked: " + hooks.size());
        } catch (Throwable t) {
            XposedBridge.log(PREFIX + "Fastjson hook failed: " + t.getClass().getSimpleName());
        }
    }
}
