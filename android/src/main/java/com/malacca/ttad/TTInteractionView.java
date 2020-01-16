package com.malacca.ttad;

import androidx.annotation.NonNull;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

class TTInteractionView extends TTadViewManager {

    @Override
    public @NonNull String getName() {
        return "RNInteractionView";
    }

    @Override
    protected @NonNull TTadView createViewInstance(@NonNull ThemedReactContext context) {
        return new TTadView(context, TTadType.INTERACTION);
    }

    // 如果是视频广告, 是否可以暂停
    @ReactProp(name = "canInterrupt")
    public void setCanInterrupt(TTadView view, boolean canInterrupt) {
        view.setCanInterrupt(canInterrupt);
    }
}