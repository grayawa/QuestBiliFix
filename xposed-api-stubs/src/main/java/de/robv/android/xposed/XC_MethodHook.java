package de.robv.android.xposed;

/** Compile-time-only signatures of the legacy Xposed API. */
public abstract class XC_MethodHook {
    protected XC_MethodHook() {}

    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {}

    public static class MethodHookParam {
        public Object thisObject;
        public Object[] args;

        public Object getResult() { throw new UnsupportedOperationException("Compile-time stub"); }
        public void setResult(Object result) { throw new UnsupportedOperationException("Compile-time stub"); }
    }

    public class Unhook {}
}
