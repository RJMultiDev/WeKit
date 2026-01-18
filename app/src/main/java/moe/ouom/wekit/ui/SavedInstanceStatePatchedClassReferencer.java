package moe.ouom.wekit.ui;

import android.content.Context;

import java.util.Objects;

import moe.ouom.wekit.util.Initiator;

public class SavedInstanceStatePatchedClassReferencer extends ClassLoader {

    private static final ClassLoader mBootstrap = Context.class.getClassLoader();
    private final ClassLoader mBaseReferencer;
    private final ClassLoader mHostReferencer;

    public SavedInstanceStatePatchedClassReferencer(ClassLoader referencer) {
        super(mBootstrap);
        mBaseReferencer = Objects.requireNonNull(referencer);
        mHostReferencer = Initiator.getHostClassLoader();
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            return Objects.requireNonNull(mBootstrap).loadClass(name);
        } catch (ClassNotFoundException ignored) {
        }
        if (mHostReferencer != null) {
            try {
                // start: overloaded
                if ("androidx.lifecycle.ReportFragment".equals(name)) {
                    return mHostReferencer.loadClass(name);
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
        // with ClassNotFoundException
        return mBaseReferencer.loadClass(name);
    }
}
