package io.github.danielkpl2.apngavif;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.Event;

final class ApngAvifEvent extends Event<ApngAvifEvent> {
    static final String LOAD = "topApngAvifLoad";
    static final String ERROR = "topApngAvifError";

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
