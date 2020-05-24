package com.malacca.ttad;

import androidx.annotation.NonNull;

import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

class TTDrawView extends TTadViewManager {

    @Override
    public @NonNull String getName() {
        return "RNTTDrawView";
    }

    @Override
    protected @NonNull TTadView createViewInstance(@NonNull ThemedReactContext context) {
        return new TTadView(context, TTadType.DRAW);
    }

    // 使用预加载 uuid
    @ReactProp(name = "uuid")
    public void setUUID(TTadView view, String uuid) {
        view.setUUID(uuid);
    }

    // 视频是否可以暂停
    @ReactProp(name = "canInterrupt")
    public void setCanInterrupt(TTadView view, boolean canInterrupt) {
        view.setCanInterrupt(canInterrupt);
    }
}