package org.jetbrains.research.intellijdeodorant;

import com.intellij.CommonBundle;
import com.intellij.reference.SoftReference;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.util.ResourceBundle;

public final class IntelliJDeodorantBundle {
    private static final String BUNDLE = "IntelliJDeodorantBundle";
    private static Reference<ResourceBundle> INSTANCE;

    private IntelliJDeodorantBundle() {
    }

    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return CommonBundle.message(getBundle(), key, params);
    }

    private static ResourceBundle getBundle() {
        ResourceBundle bundle = SoftReference.dereference(INSTANCE);
        if (bundle == null) {
            bundle = ResourceBundle.getBundle(BUNDLE);
            INSTANCE = new SoftReference<>(bundle);
        }
        return bundle;
    }
}
