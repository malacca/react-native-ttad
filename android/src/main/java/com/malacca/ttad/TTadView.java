package com.malacca.ttad;

import java.util.List;
import java.io.ByteArrayOutputStream;

import android.os.Build;
import android.view.View;
import android.util.Base64;
import android.text.TextUtils;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTImage;
import com.bytedance.sdk.openadsdk.TTFeedAd;
import com.bytedance.sdk.openadsdk.TTSplashAd;
import com.bytedance.sdk.openadsdk.TTNativeAd;
import com.bytedance.sdk.openadsdk.FilterWord;
import com.bytedance.sdk.openadsdk.TTAdDislike;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTDrawFeedAd;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
import com.bytedance.sdk.openadsdk.TTAppDownloadListener;

class TTadView extends FrameLayout implements LifecycleEventListener {
    private final static int API_LEVEL = Build.VERSION.SDK_INT;

    private TTadType adType;
    private String uuid = null;
    private String codeId = null;
    private boolean deepLink = false;
    private int timeout = 0;
    private int intervalTime = 0;
    private boolean dislikeNative = false;
    private boolean dislikeDisable = false;
    private boolean drawAdNeedLogo = false;
    private boolean canInterruptVideo = false;
    private ReadableMap listeners = null;

    private int adWidth = 0;
    private int adHeight = 0;
    private int adStatus = 0;

    private boolean adLoaded;
    private boolean jsUpdate;
    private boolean adReRender;
    private View drawClickView;
    private TTDrawFeedAd drawView;
    private TTSplashAd splashView;
    private TTNativeExpressAd adView;
    private ThemedReactContext rnContext;
    private RCTEventEmitter mEventEmitter;

    public TTadView(@NonNull Context context) {
        super(context);
    }

    public TTadView(@NonNull ThemedReactContext context, TTadType type) {
        super(context);
        adType = type;
        rnContext = context;
        mEventEmitter = context.getJSModule(RCTEventEmitter.class);
        context.addLifecycleEventListener(this);
    }

    /**
     * 插插入 addView 后需要重绘才能显示, 实测在插入 adView 后调用 `post(measureAndLayout)` 重绘也有效
     * 但对于 banner 这种可能是轮播切换的, 切换后, 又不显示了, 不晓得还有没有其他类似情况
     * 以防万一, 干脆每次 requestLayout 都重置一下, 没有深入了解 android, 眼下这个方案有效, 或许可优化
     * https://github.com/facebook/react-native/issues/17968
     * https://github.com/facebook/react-native/issues/11829
     * https://www.jianshu.com/p/a6c5042c5ce8
     */
    @Override
    public void requestLayout() {
        super.requestLayout();
        if (adLoaded) {
            post(measureAndLayout);
        }
    }

    private final Runnable measureAndLayout = new Runnable() {
        @Override
        public void run() {
            measure(
                    MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY)
            );
            layout(getLeft(), getTop(), getRight(), getBottom());
        }
    };

    // props 发生变动, 更新广告
    protected void updateAd(boolean reloadAd, boolean reloadSize) {
        // 是 jsUpdate 设置 size 的, 不处理
        if (jsUpdate) {
            jsUpdate = false;
            reloadSize = false;
        }
        if (reloadAd) {
            if (reloadSize) {
                // 重置了 ad 属性, 且 size 有变动, 这里不处理,
                // 仅重置状态值, 交给后续会触发的 onLayout 去处理
                adLoaded = false;
            } else {
                requestAd();
            }
        } else if (reloadSize && adType != TTadType.SPLASH && adType != TTadType.DRAW_NATIVE) {
            // 仅 size 变动, express 类型需重新请求, ad size 不会自适应变动
            // 同样的, 这里仅重置状态值
            adLoaded = false;
        }
    }

    // layout -> 有尺寸了 && 未Loaded -> 加载
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int width = adLoaded ? 0 : getWidth();
        int height = adLoaded ? 0 : getHeight();
        if (width == 0 || (width == adWidth && height == adHeight)) {
            return;
        }
        adWidth = width;
        adHeight = height;
        requestAd();
    }

    // android 4.4 (API Level <= 19) 在和 react-navigation tabs 一起使用时, 放在 tab 页面中
    // 切走之后再切回来, 显示为空白, 这里针对这个情况, 让 adView 重新 render() 一下
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (API_LEVEL < 21 && !adReRender && adView != null) {
            adReRender = true;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (adReRender) {
            adView.render();
        }
    }

    // 使用预加载的 uuid
    protected void setUUID(String uuid) {
        this.uuid = uuid;
    }

    // 设置广告id
    protected void setCodeId(String codeId) {
        this.codeId = codeId;
    }

    // 是否支持 deepLink
    protected void setDeepLink(boolean deepLink) {
        this.deepLink = deepLink;
    }

    // 设置开屏广告倒计时时长
    protected void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    // 设置轮播间隔时长
    protected void setIntervalTime(int intervalTime) {
        this.intervalTime = intervalTime;
        if (adView != null) {
            adView.setSlideIntervalTime(intervalTime);
        }
    }

    // 视频类型广告 是否可以暂停播放 (实测, 一旦广告渲染完成, 再修改该参数无效)
    protected void setCanInterrupt(boolean canInterrupt) {
        canInterruptVideo = canInterrupt;
        if (adType == TTadType.DRAW_NATIVE) {
            if (drawView != null) {
                drawView.setCanInterruptVideoPlay(canInterrupt);
            }
        } else if (adView != null) {
            adView.setCanInterruptVideoPlay(canInterrupt);
        }
    }

    // draw_native 类型 onLoad 回调是否需要 logo image 的 base64 数据
    protected void setNeedAdLogo(boolean needAdLogo) {
        drawAdNeedLogo = needAdLogo;
    }

    // 是否使用原生的 dislike 弹窗(弹在底部), 默认使用自定义的(弹在屏幕中间)
    protected void setDislikeNative(boolean dislikeNative) {
        this.dislikeNative = dislikeNative;
        bindDislikeListener();
    }

    // 启用 dislike, 一旦启用就不可再禁用了
    protected void setDislikeDisable(boolean dislikeDisable) {
        this.dislikeDisable = dislikeDisable;
        if (!dislikeDisable) {
            bindDislikeListener();
        }
    }

    // 设置监听事件
    protected void setListeners(ReadableMap listeners) {
        this.listeners = listeners;
        bindAdViewListener();
    }

    // 请求广告
    private void requestAd() {
        // 必须有宽度才继续进行
        if (adWidth == 0) {
            return;
        }
        adLoaded = true;

        // 使用 uuid 预加载的广告, 只有在 uuid 发生变化才重新加载, 其他情况(如尺寸/其他props)的变动直接忽略
        if (!TextUtils.isEmpty(uuid)) {
            if (adType == TTadType.DRAW_NATIVE) {
                renderNativeDrawCache(uuid);
            } else {
                renderExpressCache(uuid, adType == TTadType.DRAW);
            }
            return;
        }

        // codeId 为空, 不请求了, 直接触发 js 错误
        if (TextUtils.isEmpty(codeId)) {
            sendAdEvent("onFail", -101, "TTad codeId not defined");
            return;
        }

        TTAdManager manager = TTadModule.get();
        if (manager == null) {
            sendAdEvent("onFail", -103, "TTad sdk not initialize");
            return;
        }
        try {
            TTAdNative mTTAdNative = manager.createAdNative(rnContext);
            AdSlot.Builder builder = new AdSlot.Builder()
                    .setCodeId(codeId)
                    .setSupportDeepLink(deepLink)
                    .setAdCount(1);
            if (adType == TTadType.SPLASH) {
                loadSplashAd(mTTAdNative, builder);
            } else if (adType == TTadType.DRAW_NATIVE) {
                loadNativeDrawAd(mTTAdNative, builder);
            } else {
                loadExpressAd(mTTAdNative, builder);
            }
        } catch (Throwable e) {
            // 这里是有可能抛异常的, handle 住
            sendAdEvent("onFail", -104, e.getMessage());
        }
    }

    /**
     * splash 类型广告
     */
    private void loadSplashAd(TTAdNative mTTAdNative, AdSlot.Builder builder) {
        builder.setImageAcceptedSize(adWidth, adHeight);
        mTTAdNative.loadSplashAd(builder.build(), new TTAdNative.SplashAdListener() {
            @Override
            public void onError(int code, String msg) {
                sendAdEvent("onFail", code, msg);
            }

            @Override
            public void onTimeout() {
                sendAdEvent("onFail", -105, "request splash ad timeout");
            }

            @Override
            public void onSplashAdLoad(TTSplashAd ttSplashAd) {
                // 通知 js
                WritableMap map = Arguments.createMap();
                map.putString("event", "onLoad");
                map.putInt("type", ttSplashAd.getInteractionType());
                sendEvent(map);

                splashView = ttSplashAd;
                bindAdViewListener();
                removeAllViews();
                addView(ttSplashAd.getSplashView());
            }
        }, Math.max(timeout, 1500));
    }

    /**
     * draw 类型视频广告 自渲染模式
     */
    private void loadNativeDrawAd(TTAdNative mTTAdNative, AdSlot.Builder builder) {
        builder.setImageAcceptedSize(adWidth, adHeight);
        mTTAdNative.loadDrawFeedAd(builder.build(), new TTAdNative.DrawFeedAdListener() {
            @Override
            public void onError(int code, String message) {
                sendAdEvent("onFail", code, message);
            }

            @Override
            public void onDrawFeedAdLoad(List<TTDrawFeedAd> ads) {
                if (ads == null || ads.size() == 0) {
                    sendAdEvent("onFail", -105, "expressAd response empty");
                } else {
                    renderNativeDrawAd(ads.get(0), false);
                }
            }
        });
    }

    // 使用预加载的 native Draw 广告
    private void renderNativeDrawCache(String uuid) {
        TTDrawFeedAd ad = TTadModule.getNativeDrawPreAd(uuid);
        if (ad == null) {
            sendAdEvent("onFail", -105, "drawAd cache empty");
        } else {
            renderNativeDrawAd(ad, true);
        }
    }

    // 渲染 draw 视频广告, 该类型广告播放两遍, 会显示 按钮, 但点击按钮无法监听, 仅能监听自定义元素的点击事件
    private void renderNativeDrawAd(TTDrawFeedAd ad, final boolean preload) {
        removeAllViews();

        // 添加一个 onAdCreativeClick 的触发 view
        drawView = ad;
        drawClickView = new View(rnContext);
        drawClickView.setLayoutParams(new LayoutParams(1, 1));
        addView(drawClickView);

        ad.setCanInterruptVideoPlay(canInterruptVideo);
        ad.registerViewForInteraction(TTadView.this, drawClickView, new TTNativeAd.AdInteractionListener() {
            @Override
            public void onAdShow(TTNativeAd ad) {
                onNativeDrawAdLoaded(drawView);
                // 新版 sdk 渲染预加载的 必须要 requestLayout 才能撑开尺寸
                if (preload) {
                    requestLayout();
                }
            }

            @Override
            public void onAdClicked(View view, TTNativeAd ad) {
                // 理论上这个就不会被触发, 仅会触发下面这个
                onClickAd("onDrawAdClick");
            }

            @Override
            public void onAdCreativeClick(View view, TTNativeAd ad) {
                onClickAd("onDrawClick");
            }
        });
        bindAdViewListener();
        addView(ad.getAdView(), 0);
    }

    // 通知 js draw 广告就绪
    private void onNativeDrawAdLoaded(TTDrawFeedAd ad) {
        WritableMap map = Arguments.createMap();
        map.putString("event", "onLoad");
        map.putInt("imageMode", ad.getImageMode());
        map.putInt("type", ad.getInteractionType());
        map.putString("title", ad.getTitle());
        map.putString("description", ad.getDescription());

        if (drawAdNeedLogo) {
            Bitmap bitmap = ad.getAdLogo();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream .toByteArray();
            String logo = Base64.encodeToString(byteArray, Base64.DEFAULT);
            map.putString("logo", logo);
        }

        TTImage iconImage = ad.getIcon();
        if (iconImage == null) {
            map.putNull("icon");
        } else {
            WritableMap icon = Arguments.createMap();
            icon.putInt("width", iconImage.getWidth());
            icon.putInt("height", iconImage.getHeight());
            icon.putString("url", iconImage.getImageUrl());
            map.putMap("icon", icon);
        }

        TTImage coverImage = ad.getVideoCoverImage();
        if (coverImage == null) {
            map.putNull("cover");
        } else {
            WritableMap cover = Arguments.createMap();
            cover.putInt("width", coverImage.getWidth());
            cover.putInt("height", coverImage.getHeight());
            cover.putString("url", coverImage.getImageUrl());
            map.putMap("cover", cover);
        }
        map.putString("source", ad.getSource());
        map.putString("buttonText", ad.getButtonText());

        map.putInt("appSize", ad.getAppSize());
        map.putInt("appScore", ad.getAppScore());
        map.putInt("appCommentNum", ad.getAppCommentNum());
        map.putDouble("duration", ad.getVideoDuration());
        map.putMap("mediaExtra", Arguments.makeNativeMap(ad.getMediaExtraInfo()));
        sendEvent(map);
    }

    // 触发创意区 click 事件
    protected void clickNativeDrawAd() {
        if (drawClickView != null) {
            drawClickView.performClick();
        }
    }

    /**
     * ExpressAd 类型广告
     */
    private void loadExpressAd(TTAdNative mTTAdNative, AdSlot.Builder builder) {
        builder.setExpressViewAcceptedSize(
                TTadModule.toDIPFromPixel(adWidth),
                TTadModule.toDIPFromPixel(adHeight)
        ).setImageAcceptedSize(600, 600);
        TTAdNative.NativeExpressAdListener listener = new TTAdNative.NativeExpressAdListener() {
            @Override
            public void onError(int code, String message) {
                sendAdEvent("onFail", code, message);
            }

            @Override
            public void onNativeExpressAdLoad(List<TTNativeExpressAd> ads) {
                if (ads == null || ads.size() == 0) {
                    sendAdEvent("onFail", -105, "expressAd response empty");
                } else {
                    renderExpressAd(ads.get(0));
                }
            }
        };
        if (adType == TTadType.BANNER) {
            mTTAdNative.loadBannerExpressAd(builder.build(), listener);
        } else if (adType == TTadType.INTERACTION) {
            mTTAdNative.loadInteractionExpressAd(builder.build(), listener);
        } else if (adType == TTadType.DRAW) {
            mTTAdNative.loadExpressDrawFeedAd(builder.build(), listener);
        } else {
            mTTAdNative.loadNativeExpressAd(builder.build(), listener);
        }
    }

    // 使用预加载的 ExpressAd 广告
    private void renderExpressCache(String uuid, boolean isDraw) {
        TTNativeExpressAd ad = isDraw ? TTadModule.getDrawPreAd(uuid) : TTadModule.getFeedPreAd(uuid);
        if (ad == null) {
            sendAdEvent("onFail", -105, "expressAd cache empty");
        } else {
            renderExpressAd(ad);
        }
    }

    // 渲染 ExpressAd 广告
    private void renderExpressAd(TTNativeExpressAd ad) {
        destroyAdView();
        adView = ad;
        bindDislikeListener();
        bindAdViewListener();
        ad.setExpressInteractionListener(new TTNativeExpressAd.ExpressAdInteractionListener() {
            @Override
            public void onAdShow(View view, int type) {
                if (adReRender) {
                    adReRender = false;
                } else {
                    sendAdEvent("onShow", type, null);
                }
            }

            @Override
            public void onAdClicked(View view, int type) {
                onClickAd();
            }

            @Override
            public void onRenderFail(View view, String msg, int code) {
                sendAdEvent("onFail", code, msg);
            }

            @Override
            public void onRenderSuccess(View view, float width, float height) {
                insertAdView(view, width, height);
            }
        });
        ad.setCanInterruptVideoPlay(canInterruptVideo);
        if (intervalTime != 0) {
            ad.setSlideIntervalTime(intervalTime);
        }
        ad.render();
    }

    // 设置广告 view 视图尺寸
    private void insertAdView(final View view, final float width, final float height) {
        removeAllViews();
        addView(view);

        // 通知 js
        jsUpdate = adHeight == 0;
        WritableMap map = Arguments.createMap();
        map.putString("event", "onLoad");
        map.putInt("width", (int) width);
        map.putInt("height", (int) height);
        map.putInt("imageMode", adView.getImageMode());
        map.putInt("interaction", adView.getInteractionType());
        map.putBoolean("update", jsUpdate); // 初始高度如果为0, 通知 js 更新高度
        sendEvent(map);
    }

    // 绑定不喜欢监听
    private void bindDislikeListener() {
        if (adView == null || (adType != TTadType.FEED && adType != TTadType.BANNER) || dislikeDisable) {
            return;
        }
        // 使用默认的
        if (dislikeNative) {
            adView.setDislikeCallback(rnContext.getCurrentActivity(), new TTAdDislike.DislikeInteractionCallback() {
                @Override
                public void onSelected(int i, String s) {
                    sendAdDislikeEvent(s);
                }

                @Override
                public void onCancel() {
                }
            });
            return;
        }
        // 使用自定义的
        List<FilterWord> words = adView.getFilterWords();
        if (words == null || words.isEmpty()) {
            return;
        }
        final TTadDislikeDialog dislikeDialog = new TTadDislikeDialog(rnContext, words);
        dislikeDialog.setOnDislikeItemClick(new TTadDislikeDialog.OnDislikeItemClick() {
            @Override
            public void onItemClick(FilterWord filterWord) {
                sendAdDislikeEvent(filterWord.getName());
            }
        });
        adView.setDislikeDialog(dislikeDialog);
    }

    // 根据设置进行绑定的事件 (好蛋疼啊, 每个类型都要写一遍, 不晓得 sdk 为啥不弄一个 interface)
    private void bindAdViewListener() {
        switch (adType) {
            case SPLASH:
                bindSplashListener();
                break;
            case DRAW_NATIVE:
                bindDrawVideoListener();
                break;
            default:
                bindExpressVideoListener();
                break;
        }
    }

    // splash 广告监听
    private void bindSplashListener() {
        if (splashView == null) {
            return;
        }
        if (hasListener("bindDownload") && splashView.getInteractionType() == TTAdConstant.INTERACTION_TYPE_DOWNLOAD) {
            splashView.setDownloadListener(makeDownloadListener());
        }
        if (!hasListener("bindClick")) {
            return;
        }
        splashView.setSplashInteractionListener(new TTSplashAd.AdInteractionListener() {
            @Override
            public void onAdShow(View view, int type) {
                sendAdEvent("onShow", type, null);
            }

            @Override
            public void onAdClicked(View view, int i) {
                onClickAd();
            }

            @Override
            public void onAdSkip() {
                sendAdEvent("onSkip");
            }

            @Override
            public void onAdTimeOver() {
                sendAdEvent("onTimeOver");
            }
        });
    }

    // native draw 视频类型广告监听
    private void bindDrawVideoListener() {
        if (drawView == null) {
            return;
        }
        if (hasListener("bindDownload") && drawView.getInteractionType() == TTAdConstant.INTERACTION_TYPE_DOWNLOAD) {
            drawView.setDownloadListener(makeDownloadListener());
        }
        if (hasListener("bindClick")) {
            drawView.setDrawVideoListener(new TTDrawFeedAd.DrawVideoListener() {
                @Override
                public void onClick() {
                    onClickAd();
                }

                @Override
                public void onClickRetry() {
                    sendAdEvent("onVideoRetry");
                }
            });
        }
        if (!hasListener("bindVideo")) {
            return;
        }
        drawView.setVideoAdListener(new TTFeedAd.VideoAdListener() {
            @Override
            public void onVideoLoad(TTFeedAd ttFeedAd) {
                sendAdEvent("onVideoLoad");
            }

            @Override
            public void onVideoError(int code, int extraCode) {
                sendVideoError(code, extraCode);
            }

            @Override
            public void onVideoAdStartPlay(TTFeedAd ttFeedAd) {
                sendAdEvent("onVideoPlay");
            }

            @Override
            public void onVideoAdPaused(TTFeedAd ttFeedAd) {
                sendAdEvent("onVideoPaused");
            }

            @Override
            public void onVideoAdContinuePlay(TTFeedAd ttFeedAd) {
                sendAdEvent("onVideoContinue");
            }

            @Override
            public void onProgressUpdate(long current, long duration) {
                sendVideoProgress(current, duration);
            }

            @Override
            public void onVideoAdComplete(TTFeedAd ttFeedAd) {
                sendAdEvent("onVideoComplete");
            }
        });
    }

    // Express 视频类型广告监听  这里并不保证所有 video 都会触发
    private void bindExpressVideoListener() {
        if (adView == null) {
            return;
        }
        if (hasListener("bindDownload") && adView.getInteractionType() == TTAdConstant.INTERACTION_TYPE_DOWNLOAD) {
            adView.setDownloadListener(makeDownloadListener());
        }
        if (!hasListener("bindVideo")) {
            return;
        }
        adView.setVideoAdListener(new TTNativeExpressAd.ExpressVideoAdListener() {
            @Override
            public void onVideoLoad() {
                sendAdEvent("onVideoLoad");
            }

            @Override
            public void onVideoError(int code, int extraCode) {
                sendVideoError(code, extraCode);
            }

            @Override
            public void onVideoAdStartPlay() {
                sendAdEvent("onVideoPlay");
            }

            @Override
            public void onVideoAdPaused() {
                sendAdEvent("onVideoPaused");
            }

            @Override
            public void onProgressUpdate(long current, long duration) {
                sendVideoProgress(current, duration);
            }

            @Override
            public void onVideoAdComplete() {
                sendAdEvent("onVideoComplete");
            }

            @Override
            public void onVideoAdContinuePlay() {
                sendAdEvent("onVideoContinue");
            }

            @Override
            public void onClickRetry() {
                sendAdEvent("onVideoRetry");
            }
        });
    }

    // 创建下载监听器
    private TTAppDownloadListener makeDownloadListener() {
        return new TTAppDownloadListener() {
            @Override
            public void onIdle() {
                sendDownloadEvent("onIdle", null, null, -1, -1);
            }

            @Override
            public void onDownloadActive(long totalBytes, long currBytes, String fileName, String appName) {
                sendDownloadEvent("onDownloadActive", fileName, appName, totalBytes, currBytes);
            }

            @Override
            public void onDownloadPaused(long totalBytes, long currBytes, String fileName, String appName) {
                sendDownloadEvent("onDownloadPaused", fileName, appName, totalBytes, currBytes);
            }

            @Override
            public void onDownloadFailed(long totalBytes, long currBytes, String fileName, String appName) {
                sendDownloadEvent("onDownloadFailed", fileName, appName, totalBytes, currBytes);
            }

            @Override
            public void onDownloadFinished(long totalBytes, String fileName, String appName) {
                sendDownloadEvent("onDownloadFinished", fileName, appName, totalBytes, -1);
            }

            @Override
            public void onInstalled(String fileName, String appName) {
                sendDownloadEvent("onInstalled", fileName, appName, -1, -1);
            }
        };
    }

    // 点击广告了
    private void onClickAd() {
        onClickAd("onClick");
    }

    private void onClickAd(String event) {
        adStatus = 1;
        sendAdEvent(event);
    }

    @Override
    public void onHostPause() {
        // 是在点击之后 pause 的, 触发一个 onOpen 事件
        // 可能是跳到广告h5页面 或 app安装页
        if (adStatus == 1) {
            adStatus = 2;
            sendAdEvent("onOpen");
        }
    }

    @Override
    public void onHostResume() {
        // 从 pause 转过来的, 触发一个 onClose 事件
        if (adStatus == 2) {
            sendAdEvent("onClose");
        }
        adStatus = 0;
    }

    @Override
    public void onHostDestroy() {
        destroyAdView();
    }

    public void destroyAdView() {
        if (adView != null) {
            adView.destroy();
        }
    }

    private boolean hasListener(String event) {
        return listeners != null && listeners.hasKey(event) && listeners.getBoolean(event);
    }

    private void sendAdEvent(String event) {
        if (hasListener(event)) {
            sendAdEvent(event, 0, null);
        }
    }

    private void sendAdEvent(String event, int code, @Nullable String error) {
        if (hasListener(event)) {
            WritableMap params = Arguments.createMap();
            params.putString("event", event);
            params.putInt("code", code);
            params.putString("error", error);
            sendEvent(params);
        }
    }

    private void sendAdDislikeEvent(String dislike) {
        WritableMap params = Arguments.createMap();
        params.putString("event", "onDislike");
        params.putString("dislike", dislike);
        sendEvent(params);
    }

    private void sendVideoError(int code, int extraCode) {
        if (!hasListener("onVideoError")) {
            return;
        }
        WritableMap params = Arguments.createMap();
        params.putString("event", "onVideoError");
        params.putInt("code", code);
        params.putInt("extraCode", extraCode);
        sendEvent(params);
    }

    private void sendVideoProgress(long current, long duration) {
        if (!hasListener("onVideoProgress")) {
            return;
        }
        WritableMap params = Arguments.createMap();
        params.putString("event", "onVideoProgress");
        params.putDouble("current", current);
        params.putDouble("duration", duration);
        sendEvent(params);
    }

    private void sendDownloadEvent(String event, String fileName, String appName, long totalBytes, long currBytes) {
        if (hasListener(event)) {
            WritableMap params = Arguments.createMap();
            params.putString("event", event);
            params.putString("fileName", fileName);
            params.putString("appName", appName);
            params.putDouble("totalBytes", totalBytes);
            params.putDouble("currBytes", currBytes);
            sendEvent(params);
        }
    }

    private void sendEvent(WritableMap event) {
        mEventEmitter.receiveEvent(getId(), TTadViewManager.EVENT_NAME, event);
    }
}