package moe.ouom.wekit.core.model;

import android.content.Context;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XC_MethodHook;
import moe.ouom.wekit.loader.startup.HybridClassLoader;
import moe.ouom.wekit.util.common.SyncUtils;
import moe.ouom.wekit.util.log.WeLogger;

public abstract class BaseClickableFunctionHookItem extends BaseHookItem {

    private boolean enabled;
    private final int targetProcess = targetProcess();
    private final boolean alwaysRun = alwaysRun();
    private boolean isLoaded = false;

    /**
     * 目标进程
     */
    public int targetProcess() {
        return SyncUtils.PROC_MAIN;
    }


    public int getTargetProcess() {
        return targetProcess;
    }

    public boolean alwaysRun() {
        return false;
    }

    public boolean getAlwaysRun() {
        return alwaysRun;
    }



    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;

        this.enabled = enabled;
        if (!enabled) {
            if (isLoaded) {
                WeLogger.i("[BaseClickableFunctionHookItem] Unloading HookItem: " + getPath());
                try {
                    this.unload(HybridClassLoader.getHostClassLoader());
                    isLoaded = false;
                } catch (Throwable e) {
                    WeLogger.e("[BaseClickableFunctionHookItem] Unload HookItem Failed", e);
                }
            }
        } else {
            WeLogger.i("[BaseClickableFunctionHookItem] Loading HookItem: " + getPath());
            this.startLoad();
            isLoaded = true;
        }
    }

    protected final void tryExecute(@NonNull XC_MethodHook.MethodHookParam param, @NonNull HookAction hookAction) {
        if (isEnabled()) {
            super.tryExecute(param, hookAction);
        }
    }

    public boolean noSwitchWidget() {
        return false;
    }

    /**
     * 点击事件处理
     *
     * @param context 上下文
     */
    public abstract void onClick(Context context);

}
