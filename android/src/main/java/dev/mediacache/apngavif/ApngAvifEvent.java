package dev.mediacache.apngavif;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.Event;

final class ApngAvifEvent extends Event<ApngAvifEvent> {
    // The new architecture maps these to the onLoad and onError props by name.
    static final String LOAD = "topLoad";
    static final String ERROR = "topError";

    private final String name;
    private final WritableMap data;

    ApngAvifEvent(int surfaceId, int viewTag, String name, WritableMap data) {
        super(surfaceId, viewTag);
        this.name = name;
        this.data = data;
    }

    @Override
    public String getEventName() {
        return name;
    }

    @Nullable
    @Override
    protected WritableMap getEventData() {
        return data;
    }
}
