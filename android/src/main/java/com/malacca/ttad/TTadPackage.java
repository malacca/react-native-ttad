package com.malacca.ttad;

import java.util.List;
import java.util.Arrays;
import java.util.Collections;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.react.bridge.ReactApplicationContext;

import androidx.annotation.NonNull;

public class TTadPackage implements ReactPackage {

    @NonNull
    @Override
    public List<NativeModule> createNativeModules(@NonNull ReactApplicationContext reactContext) {
        return Collections.<NativeModule>singletonList(new TTadModule(reactContext));
    }

    @NonNull
    @Override
    public List<ViewManager> createViewManagers(@NonNull ReactApplicationContext reactContext) {
        return Arrays.<ViewManager>asList(
                new TTBannerView(),
                new TTFeedView(),
                new TTInteractionView(),
                new TTSplashView(),
                new TTDrawView(),
                new TTDrawNativeView()
        );
    }
}
