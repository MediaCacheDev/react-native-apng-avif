package io.github.danielkpl2.apngavif;

import android.view.View;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.uimanager.SimpleViewManager;

/**
 * Old architecture: the same methods as the interface codegen generates for the new one, so the
 * manager compiles against either. Props arrive through its {@code @ReactProp} setters.
 */
public abstract class ApngAvifViewManagerSpec<T extends View> extends SimpleViewManager<T> {
    public abstract void setSource(T view, @Nullable ReadableMap value);
    public abstract void setAutoPlay(T view, boolean value);
    public abstract void setPaused(T view, boolean value);
    public abstract void setLoopCount(T view, int value);
    public abstract void setResizeMode(T view, @Nullable String value);
}
