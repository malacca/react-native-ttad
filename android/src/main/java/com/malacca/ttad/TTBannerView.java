package com.malacca.ttad;

import androidx.annotation.NonNull;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

class TTBannerView extends TTadViewManager {

    @Override
    public @NonNull String getName() {
        return "RNTTBannerView";
    }

    @Override
    protected @NonNull TTadView createViewInstance(@NonNull ThemedReactContext context) {
        return new TTadView(context, TTadType.BANNER);
    }

    // banner 轮播间隔时长
    @ReactProp(name = "intervalTime")
    public void setIntervalTime(TTadView view, int intervalTime) {
        view.setIntervalTime(intervalTime);
    }

    // 如果是视频广告, 是否可以暂停
    @ReactProp(name = "canInterrupt")
    public void setCanInterrupt(TTadView view, boolean canInterrupt) {
        view.setCanInterrupt(canInterrupt);
    }

    // 使用原生的 dislike 弹层
    @ReactProp(name = "dislikeNative")
    public void setDislikeNative(TTadView view, boolean dislikeNative) {
        view.setDislikeNative(dislikeNative);
    }

    // 禁用 dislike
    @ReactProp(name = "dislikeDisable")
    public void setDislikeDisable(TTadView view, boolean dislikeDisable) {
        view.setDislikeDisable(dislikeDisable);
    }
}