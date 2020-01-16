package com.malacca.ttad;

import androidx.annotation.NonNull;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

class TTSplashView extends TTadViewManager {

    @Override
    public @NonNull String getName() {
        return "RNTTSplashView";
    }

    @Override
    protected @NonNull TTadView createViewInstance(@NonNull ThemedReactContext context) {
        return new TTadView(context, TTadType.SPLASH);
    }

    // 加载超时时长
    @ReactProp(name = "timeout")
    public void setTimeout(TTadView view, int timeout) {
        view.setTimeout(timeout);
    }
}