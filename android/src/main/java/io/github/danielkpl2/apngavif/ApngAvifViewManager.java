package io.github.danielkpl2.apngavif;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.HashMap;
import java.util.Map;

public class ApngAvifViewManager extends ApngAvifViewManagerSpec<ApngAvifView> {
    public static final String REACT_CLASS = "RNApngAvifView";

    @NonNull
    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @NonNull
    @Override
    protected ApngAvifView createViewInstance(@NonNull ThemedReactContext context) {
        return new ApngAvifView(context);
    }

    @Override
    public void onDropViewInstance(@NonNull ApngAvifView view) {
        super.onDropViewInstance(view);
        view.release();
    }

    @ReactProp(name = "source")
    @Override
    public void setSource(ApngAvifView view, @Nullable ReadableMap source) {
        view.setSource(string(source, "uri"), string(source, "cacheKey"));
    }

    @ReactProp(name = "autoPlay", defaultBoolean = true)
    @Override
    public void setAutoPlay(ApngAvifView view, boolean value) {
        view.setAutoPlay(value);
    }

    @ReactProp(name = "paused", defaultBoolean = false)
    @Override
    public void setPaused(ApngAvifView view, boolean value) {
        view.setPaused(value);
    }

    @ReactProp(name = "loopCount", defaultInt = 0)
    @Override
    public void setLoopCount(ApngAvifView view, int value) {
        view.setLoopCount(value);
    }

    @ReactProp(name = "resizeMode")
    @Override
    public void setResizeMode(ApngAvifView view, @Nullable String value) {
        view.setResizeMode(value);
    }

    @Nullable
    @Override
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        Map<String, Object> events = new HashMap<>();
        events.put(ApngAvifEvent.LOAD, registration("onLoad"));
        events.put(ApngAvifEvent.ERROR, registration("onError"));
        return events;
    }

    private static Map<String, String> registration(String name) {
        Map<String, String> registration = new HashMap<>();
        registration.put("registrationName", name);
        return registration;
    }

    @Nullable
    private static String string(@Nullable ReadableMap map, String key) {
        return map != null && map.hasKey(key) && !map.isNull(key) ? map.getString(key) : null;
    }
}
