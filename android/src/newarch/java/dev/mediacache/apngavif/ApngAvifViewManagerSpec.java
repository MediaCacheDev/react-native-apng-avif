package dev.mediacache.apngavif;

import android.view.View;

import androidx.annotation.Nullable;

import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ViewManagerDelegate;
import com.facebook.react.viewmanagers.RNApngAvifViewManagerDelegate;
import com.facebook.react.viewmanagers.RNApngAvifViewManagerInterface;

/** New architecture: props arrive through the delegate generated from the codegen spec. */
public abstract class ApngAvifViewManagerSpec<T extends View> extends SimpleViewManager<T>
        implements RNApngAvifViewManagerInterface<T> {

    private final ViewManagerDelegate<T> delegate = new RNApngAvifViewManagerDelegate<>(this);

    @Nullable
    @Override
    protected ViewManagerDelegate<T> getDelegate() {
        return delegate;
    }
}
