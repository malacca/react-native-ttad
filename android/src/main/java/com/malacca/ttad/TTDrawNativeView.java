package com.malacca.ttad;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

// 自渲染类型 draw 广告,  默认已无法申请, 之前做好的, 就保留了
class TTDrawNativeView extends TTadViewManager {

    @Override
    public @NonNull String getName() {
        return "RNTTDrawNativeView";
    }

    @Override
    protected @NonNull TTadView createViewInstance(@NonNull ThemedReactContext context) {
        return new TTadView(context, TTadType.DRAW_NATIVE);
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

    // 是否返回 穿山甲 logo 的 base64
    @ReactProp(name = "needAdLogo")
    public void setNeedAdLogo(TTadView view, boolean needAdLogo) {
        view.setNeedAdLogo(needAdLogo);
    }

    // 暴露给 js 的接口, 触发 draw ad 的点击
    @Override
    public void receiveCommand(@NonNull TTadView view, String commandId, @Nullable ReadableArray args) {
        if ("clickDraw".equals(commandId)) {
            view.clickNativeDrawAd();
        }
    }
}