package moe.ouom.wekit.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.view.ContextThemeWrapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

import moe.ouom.wekit.R;
import moe.ouom.wekit.util.common.ModuleRes;

/**
 * 纯净的 ContextWrapper，不注入任何资源，仅用于处理 Theme 和 ClassLoader
 * 配合 ModuleRes 使用可实现 不注入资源 加载模块界面
 */
public class CommonContextWrapper extends ContextThemeWrapper {

    public CommonContextWrapper(@NonNull Context base, int theme) {
        this(base, theme, null);
    }

    public CommonContextWrapper(@NonNull Context base, int theme,
                                @Nullable Configuration configuration) {
        super(base, theme);
        if (configuration != null) {
            mOverrideResources = base.createConfigurationContext(configuration).getResources();
        }
    }

    private ClassLoader mXref = null;
    private Resources mOverrideResources;

    @NonNull
    @Override
    public ClassLoader getClassLoader() {
        if (mXref == null) {
            mXref = new SavedInstanceStatePatchedClassReferencer(
                    CommonContextWrapper.class.getClassLoader());
        }
        return mXref;
    }

    @Nullable
    private static Configuration recreateNighModeConfig(@NonNull Context base, int uiNightMode) {
        Objects.requireNonNull(base, "base is null");
        Configuration baseConfig = base.getResources().getConfiguration();
        if ((baseConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK) == uiNightMode) {
            return null;
        }
        Configuration conf = new Configuration();
        conf.uiMode = uiNightMode | (baseConfig.uiMode & ~Configuration.UI_MODE_NIGHT_MASK);
        return conf;
    }

    @NonNull
    @Override
    public Resources getResources() {
        if (mOverrideResources == null) {
            return super.getResources();
        } else {
            return mOverrideResources;
        }
    }

    public static boolean isAppCompatContext(@NonNull Context context) {
        if (!checkContextClassLoader(context)) {
            return false;
        }
        TypedArray a = context.obtainStyledAttributes(androidx.appcompat.R.styleable.AppCompatTheme);
        try {
            return a.hasValue(androidx.appcompat.R.styleable.AppCompatTheme_windowActionBar);
        } finally {
            a.recycle();
        }
    }

    private static final int[] MATERIAL_CHECK_ATTRS = {com.google.android.material.R.attr.colorPrimaryVariant};

    public static boolean isMaterialDesignContext(@NonNull Context context) {
        if (!isAppCompatContext(context)) {
            return false;
        }
        @SuppressLint("ResourceType") TypedArray a = context.obtainStyledAttributes(MATERIAL_CHECK_ATTRS);
        try {
            return a.hasValue(0);
        } finally {
            a.recycle();
        }
    }

    public static boolean checkContextClassLoader(@NonNull Context context) {
        try {
            ClassLoader cl = context.getClassLoader();
            if (cl == null) {
                return false;
            }
            return cl.loadClass(AppCompatActivity.class.getName()) == AppCompatActivity.class;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @NonNull
    public static Context createAppCompatContext(@NonNull Context base) {
        if (isAppCompatContext(base)) {
            return base;
        }
        return new CommonContextWrapper(base, R.style.Theme_Wekit,
                recreateNighModeConfig(base, getNightModeMasked(base)));
    }

    // ----------------- 夜间模式相关 -----------------

    public static boolean isInNightMode(@NonNull Context context) {
        int mode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    public static int getNightModeMasked(@NonNull Context context) {
        return isInNightMode(context) ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO;
    }

    /**
     * 兼容性方法：尝试使用模块上下文判断，如果没有则默认返回 NO
     */
    public static int getNightModeMasked() {
        Context moduleCtx = ModuleRes.getContext();
        return (moduleCtx != null) ? getNightModeMasked(moduleCtx) : Configuration.UI_MODE_NIGHT_NO;
    }

    /**
     * 兼容性方法
     */
    public static boolean isInNightMode() {
        return getNightModeMasked() == Configuration.UI_MODE_NIGHT_YES;
    }

    @NonNull
    public static Context createMaterialDesignContext(@NonNull Context base) {
        if (isMaterialDesignContext(base)) {
            return base;
        }
        return createAppCompatContext(base);
    }
}