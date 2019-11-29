package com.malacca.ttad;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.common.MapBuilder;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.annotations.ReactProp;

public abstract class TTadViewManager extends SimpleViewManager<TTadView> {
    public static final String EVENT_NAME = "onTTadViewEvent";

    @Override
    public void onDropViewInstance(@NonNull TTadView view) {
        super.onDropViewInstance(view);
        view.destroyAdView();
    }

    @ReactProp(name = "codeId")
    public void setCodeId(TTadView view, String codeId) {
        view.setCodeId(codeId);
    }

    @ReactProp(name = "deepLink")
    public void setDeepLink(TTadView view, Boolean deepLink) {
        view.setDeepLink(deepLink);
    }

    @ReactProp(name = "listeners")
    public void setListeners(TTadView view, ReadableMap listeners) {
        view.setListeners(listeners);
    }

    @Override
    public @Nullable Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.<String, Object>builder()
                .put(EVENT_NAME, MapBuilder.of("registrationName", EVENT_NAME))
                .build();
    }
}
