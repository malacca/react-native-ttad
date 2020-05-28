package com.malacca.ttad;

import java.util.List;
import java.util.UUID;
import java.util.HashMap;

import android.view.View;
import android.app.Activity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.content.res.Resources;
import android.content.pm.ApplicationInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAdConfig;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTDrawFeedAd;
import com.bytedance.sdk.openadsdk.TTRewardVideoAd;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
import com.bytedance.sdk.openadsdk.TTFullScreenVideoAd;
import com.bytedance.sdk.openadsdk.TTAppDownloadListener;

/**
 * 处理 ttad sdk 的初始化
 * 桥接 全屏视频/激励视频/插屏广告/Feed信息流预加载/draw视频流预加载 接口
 */
class TTadModule extends ReactContextBaseJavaModule implements LifecycleEventListener {
    private static boolean sdkInit = false;
    private static final String REACT_CLASS = "TTadModule";
    private static TTAdConstant.RitScenes[] mttRitScenes = {
        TTAdConstant.RitScenes.HOME_OPEN_BONUS,
        TTAdConstant.RitScenes.HOME_SVIP_BONUS,
        TTAdConstant.RitScenes.HOME_GET_PROPS,
        TTAdConstant.RitScenes.HOME_TRY_PROPS,
        TTAdConstant.RitScenes.HOME_GET_BONUS,
        TTAdConstant.RitScenes.HOME_GIFT_BONUS,
        TTAdConstant.RitScenes.GAME_START_BONUS,
        TTAdConstant.RitScenes.GAME_REDUCE_WAITING,
        TTAdConstant.RitScenes.GAME_MORE_OPPORTUNITIES,
        TTAdConstant.RitScenes.GAME_FINISH_REWARDS,
        TTAdConstant.RitScenes.GAME_GIFT_BONUS,
        TTAdConstant.RitScenes.CUSTOMIZE_SCENES,
    };
    private static HashMap<String, TTNativeExpressAd> feedAds = new HashMap<>();
    private static HashMap<String, TTNativeExpressAd> drawAds = new HashMap<>();
    private static HashMap<String, TTDrawFeedAd> nativeDrawAds = new HashMap<>();

    private ReactApplicationContext reactContext;
    private HashMap<String,TTRewardVideoAd> mttRewardVideoAds = new HashMap<>();
    private HashMap<String,TTFullScreenVideoAd> mttFullVideoAds = new HashMap<>();

    private String lastClickInteraction = null;
    private String lastActiveInteraction = null;
    private boolean lastInteractionDismiss = true;
    private HashMap<String, String[]> mttInteractionErrs = new HashMap<>();
    private HashMap<String, ReadableMap> mttInteractionConfigs = new HashMap<>();
    private HashMap<String,TTNativeExpressAd> mttInteractionAds = new HashMap<>();

    private DeviceEventManagerModule.RCTDeviceEventEmitter mJSModule = null;

    // pix -> dip
    private static DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
    static int toDIPFromPixel(float value) {
        return (int) (value / displayMetrics.density);
    }

    // 请求广告的接口类
    static @Nullable TTAdManager get() {
        return sdkInit ? TTAdSdk.getAdManager() : null;
    }

    // 获取预加载的 feed 类型广告, 只能取一次, 取完后就会从缓存中移除
    static TTNativeExpressAd getFeedPreAd(String uuid) {
        if (feedAds.containsKey(uuid)) {
            TTNativeExpressAd ad = feedAds.get(uuid);
            feedAds.remove(uuid);
            return ad;
        }
        return null;
    }

    // 获取预加载的 draw 类型广告, 只能取一次, 取完后就会从缓存中移除
    static TTNativeExpressAd getDrawPreAd(String uuid) {
        if (drawAds.containsKey(uuid)) {
            TTNativeExpressAd ad = drawAds.get(uuid);
            drawAds.remove(uuid);
            return ad;
        }
        return null;
    }

    // 获取预加载的 native draw 类型广告, 只能取一次, 取完后就会从缓存中移除
    static TTDrawFeedAd getNativeDrawPreAd(String uuid) {
        if (nativeDrawAds.containsKey(uuid)) {
            TTDrawFeedAd ad = nativeDrawAds.get(uuid);
            nativeDrawAds.remove(uuid);
            return ad;
        }
        return null;
    }

    /**
     * 模块类 开始
     */
    TTadModule(ReactApplicationContext context) {
        super(context);
        reactContext = context;
        reactContext.addLifecycleEventListener(this);
    }

    @Override
    public @NonNull String getName() {
        return REACT_CLASS;
    }

    /**
     * 初始化 sdk
     */
    @ReactMethod
    public void initSdk(ReadableMap config) {
        if (sdkInit) {
            return;
        }
        sdkInit = true;
        String appId = config.hasKey("appId") ? config.getString("appId") : null;
        String appName = config.hasKey("appName") ? config.getString("appName") : null;
        if (TextUtils.isEmpty(appName)) {
            ApplicationInfo applicationInfo = reactContext.getApplicationInfo();
            int stringId = applicationInfo.labelRes;
            appName = stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : reactContext.getString(stringId);
        }
        if (TextUtils.isEmpty(appId) || TextUtils.isEmpty(appName)) {
            return;
        }
        boolean showNotify = !config.hasKey("showNotify") || config.getBoolean("showNotify");
        boolean download4g = config.hasKey("download4g") && config.getBoolean("download4g");
        boolean lightBar = config.hasKey("lightBar") && config.getBoolean("lightBar");
        boolean debug = config.hasKey("debug") && config.getBoolean("debug") || BuildConfig.DEBUG;

        final TTAdConfig.Builder builder = new TTAdConfig.Builder()
                .appId(appId)
                .appName(appName)
                .titleBarTheme(lightBar ? TTAdConstant.TITLE_BAR_THEME_LIGHT : TTAdConstant.TITLE_BAR_THEME_DARK)
                .allowShowNotify(showNotify) //是否允许sdk展示通知栏提示
                .useTextureView(true) //使用TextureView控件播放视频,默认为SurfaceView,测试发现不开启这个, 点广告再回来,视频黑屏
                .allowShowPageWhenScreenLock(false) //不支持锁屏场景广告, 这功能太猥琐了
                .supportMultiProcess(false) //是否支持多进程
                .debug(debug); //测试阶段打开，可以通过日志排查问题，上线时去除该调用

        //允许直接下载的网络状态集合
        if (download4g) {
            builder.directDownloadNetworkType(TTAdConstant.NETWORK_STATE_WIFI, TTAdConstant.NETWORK_STATE_4G);
        } else {
            builder.directDownloadNetworkType(TTAdConstant.NETWORK_STATE_WIFI);
        }
        TTAdSdk.init(reactContext, builder.build());
    }

    /**
     * 预加载 feed 类型广告, 可获取广告 id 用于 <TTFeed/> 标签
     * config = {
     *     hash:String,    标志符
     *     codeId:String,  广告id
     *     deepLink: bool, 是否支持 deepLink, 默认 true
     *     permission:bool, 是否请求必要的权限, 建议在载入广告前就先请求好权限
     *     width: float, 请求宽度 不指定则使用屏幕宽度
     *     height: float, 请求高度 可不指定, 会自动根据在广告管理后台的比例加载
     *     count: int, 预加载条数
     * }
     */
    @ReactMethod
    public void loadFeed(ReadableMap config) {
        loadTTAd(config, TTadType.FEED);
    }

    /**
     * 预加载 draw 类型广告, 可获取广告 id 用于 <TTDraw/> 标签
     * config 配置与 loadFeed 相同
     */
    @ReactMethod
    public void loadDraw(ReadableMap config) {
        loadTTAd(config, TTadType.DRAW);
    }

    /**
     * 加载 全屏视频
     * config = {
     *     hash:String,    标志符
     *     codeId:String,  广告id
     *     horizontal: bool, 是否横屏, 默认 false
     *     deepLink: bool, 是否支持 deepLink, 默认 true
     *     permission:bool, 是否请求必要的权限, 建议在载入广告前就先请求好权限
     * }
     */
    @ReactMethod
    public void loadFullVideo(ReadableMap config) {
        loadTTAd(config, TTadType.FULL);
    }

    /**
     * 显示 全屏视频
     */
    @ReactMethod
    public void showFullVideo(
            final String hash,
            final ReadableMap config,
            final int ritScenes,
            final @Nullable String scenes
    ) {
        if (TextUtils.isEmpty(hash)) {
            return;
        }
        final Activity mainActivity = getCurrentActivity();
        if (mainActivity == null) {
            sendAdEvent(hash, "onVideoUnPlay", -106, "get fullScreenVideo activity failed");
            return;
        }
        final TTFullScreenVideoAd ad = mttFullVideoAds.containsKey(hash) ? mttFullVideoAds.get(hash) : null;
        if (ad == null) {
            sendAdEvent(hash, "onVideoUnPlay", -107, "fullScreenVideo not loaded");
            return;
        }
        if (isConfigTrue(config, "showDownLoadBar")) {
            ad.setShowDownLoadBar(true);
        }
        TTAppDownloadListener downloadListener = ad.getInteractionType() == TTAdConstant.INTERACTION_TYPE_DOWNLOAD
                ? getDownloadListener(hash, config) : null;
        if (downloadListener != null) {
            ad.setDownloadListener(downloadListener);
        }
        ad.setFullScreenVideoAdInteractionListener(new TTFullScreenVideoAd.FullScreenVideoAdInteractionListener() {
            @Override
            public void onAdShow() {
                if (isConfigTrue(config, "onVideoShow")) {
                    sendAdEvent(hash, "onVideoShow");
                }
            }

            @Override
            public void onAdVideoBarClick() {
                if (isConfigTrue(config, "onVideoClick")) {
                    sendAdEvent(hash, "onVideoClick");
                }
            }

            @Override
            public void onAdClose() {
                if (isConfigTrue(config, "onVideoClose")) {
                    sendAdEvent(hash, "onVideoClose");
                }
            }

            @Override
            public void onVideoComplete() {
                if (isConfigTrue(config, "onVideoComplete")) {
                    sendAdEvent(hash, "onVideoComplete");
                }
            }

            @Override
            public void onSkippedVideo() {
                if (isConfigTrue(config, "onVideoSkip")) {
                    sendAdEvent(hash, "onVideoSkip");
                }
            }
        });
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ad.showFullScreenVideoAd(mainActivity, mttRitScenes[ritScenes], scenes);
                mttFullVideoAds.remove(hash);
            }
        });
    }

    /**
     * 加载 激励视频
     * 全屏视频 config + {
     *     userId:String,  站内用户ID
     *     rewardName:String, 激励名称(金币)
     *     rewardAmount:int,  激励数量
     *     extra: String, 额外信息, 服务端接口可获取
     * }
     */
    @ReactMethod
    public void loadRewardVideo(ReadableMap config) {
        loadTTAd(config, TTadType.REWARD);
    }

    /**
     * 显示 激励视频
     */
    @ReactMethod
    public void showRewardVideo(
            final String hash,
            final ReadableMap config,
            final int ritScenes,
            final @Nullable String scenes
    ) {
        if (TextUtils.isEmpty(hash)) {
            return;
        }
        final Activity mainActivity = getCurrentActivity();
        if (mainActivity == null) {
            sendAdEvent(hash, "onVideoUnPlay", -106, "get rewardVideo activity failed");
            return;
        }
        final TTRewardVideoAd ad = mttRewardVideoAds.containsKey(hash) ? mttRewardVideoAds.get(hash) : null;
        if (ad == null) {
            sendAdEvent(hash, "onVideoUnPlay", -107, "rewardVideo not loaded");
            return;
        }
        if (isConfigTrue(config, "showDownLoadBar")) {
            ad.setShowDownLoadBar(true);
        }
        TTAppDownloadListener downloadListener = ad.getInteractionType() == TTAdConstant.INTERACTION_TYPE_DOWNLOAD
                ? getDownloadListener(hash, config) : null;
        if (downloadListener != null) {
            ad.setDownloadListener(downloadListener);
        }
        ad.setRewardAdInteractionListener(new TTRewardVideoAd.RewardAdInteractionListener() {
            @Override
            public void onAdShow() {
                if (isConfigTrue(config, "onVideoShow")) {
                    sendAdEvent(hash, "onVideoShow");
                }
            }

            @Override
            public void onAdVideoBarClick() {
                if (isConfigTrue(config, "onVideoClick")) {
                    sendAdEvent(hash, "onVideoClick");
                }
            }

            @Override
            public void onAdClose() {
                if (isConfigTrue(config, "onVideoClose")) {
                    sendAdEvent(hash, "onVideoClose");
                }
            }

            @Override
            public void onVideoComplete() {
                if (isConfigTrue(config, "onVideoComplete")) {
                    sendAdEvent(hash, "onVideoComplete");
                }
            }

            @Override
            public void onSkippedVideo() {
                if (isConfigTrue(config, "onVideoSkip")) {
                    sendAdEvent(hash, "onVideoSkip");
                }
            }

            @Override
            public void onVideoError() {
                sendAdEvent(hash, "onVideoUnPlay", -108, "rewardVideo play failed");
            }

            //视频播放完成后，奖励验证回调，rewardVerify：是否有效，rewardAmount：奖励梳理，rewardName：奖励名称
            @Override
            public void onRewardVerify(boolean rewardVerify, int rewardAmount, String rewardName) {
                if (!isConfigTrue(config, "onRewardVerify")) {
                    return;
                }
                WritableMap params = Arguments.createMap();
                params.putString("hash", hash);
                params.putString("event", "onRewardVerify");
                params.putBoolean("verify", rewardVerify);
                params.putString("name", rewardName);
                params.putInt("amount", rewardAmount);
                sendEvent(params);
            }
        });
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ad.showRewardVideoAd(mainActivity, mttRitScenes[ritScenes], scenes);
                mttRewardVideoAds.remove(hash);
            }
        });
    }

    /**
     * 加载 插屏广告
     * config = {
     *     hash:String,    标志符
     *     codeId:String,  广告id
     *     deepLink: bool, 是否支持 deepLink, 默认 true
     *     permission:bool, 是否请求必要的权限, 建议在载入广告前就先请求好权限
     *     width: float, 请求宽度 默认为屏幕宽度的 4/5
     *     height: float, 请求高度 高度可不指定, 会自动根据在广告管理后台的比例加载
     * }
     */
    @ReactMethod
    public void loadInteraction(ReadableMap config) {
        loadTTAd(config, TTadType.INTERACTION);
    }

    /**
     * 显示 插屏广告
     */
    @ReactMethod
    public void showInteraction(final String hash, final ReadableMap config) {
        if (TextUtils.isEmpty(hash)) {
            return;
        }
        final Activity mainActivity = getCurrentActivity();
        if (mainActivity == null) {
            sendAdEvent(hash, "onVideoUnPlay", -106, "get interactionAd activity failed");
            return;
        }
        String[] errors = mttInteractionErrs.containsKey(hash) ? mttInteractionErrs.get(hash) : null;
        if (errors != null) {
            int code = Integer.parseInt(errors[0]);
            sendAdEvent(hash, "onVideoUnPlay", code, errors[1]);
            mttInteractionErrs.remove(hash);
            return;
        }
        final TTNativeExpressAd ad = mttInteractionAds.containsKey(hash) ? mttInteractionAds.get(hash) : null;
        if (ad == null) {
            sendAdEvent(hash, "onVideoUnPlay", -107, "interactionAd not loaded");
            return;
        }
        TTAppDownloadListener downloadListener = ad.getInteractionType() == TTAdConstant.INTERACTION_TYPE_DOWNLOAD
                ? getDownloadListener(hash, config) : null;
        if (downloadListener != null) {
            ad.setDownloadListener(downloadListener);
        }
        // 缓存插屏配置
        mttInteractionConfigs.put(hash, config);
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ad.showInteractionExpressAd(mainActivity);
                mttInteractionAds.remove(hash);
            }
        });
    }

    /**
     * 加载广告
     */
    private void loadTTAd(ReadableMap config, TTadType type) {
        // 参数检测
        final String hash = getConfigStr(config, "hash");
        if (TextUtils.isEmpty(hash)) {
            return;
        }
        String codeId = getConfigStr(config, "codeId");
        if (TextUtils.isEmpty(codeId)) {
            sendAdEvent(hash, "onVideoError", -101, "TTad codeId not defined");
            return;
        }

        final Activity mainActivity = getCurrentActivity();

        //step1: 初始化sdk
        TTAdManager ttAdManager = mainActivity == null ? null : get();
        if (ttAdManager == null) {
            sendAdEvent(hash, "onVideoError", -103, "TTad sdk not initialize");
            return;
        }

        //step2: 请求权限
        if (isConfigTrue(config, "permission")) {
            ttAdManager.requestPermissionIfNecessary(mainActivity);
        }

        //step3: 创建TTAdNative对象,用于调用广告请求接口
        TTAdNative mTTAdNative = ttAdManager.createAdNative(mainActivity);

        // 广告尺寸处理
        int width, height;
        int adWidth = config.hasKey("width") ? config.getInt("width") : 0;
        int adHeight = config.hasKey("height") ? config.getInt("height") : 0;

        // 未指尺寸, 使用屏幕宽度, 插屏使用屏幕宽度的 4/5
        if (adWidth == 0) {
            width = type == TTadType.INTERACTION ? displayMetrics.widthPixels * 4 / 5 : displayMetrics.widthPixels;
            adWidth = toDIPFromPixel(width);
        } else {
            width = (int) (adWidth * displayMetrics.density);
        }

        // adHeight 可以为0, 但 height 必须指定, 虽然不起作用
        height = adHeight == 0 ? (type == TTadType.INTERACTION ? width / 3 * 2 : displayMetrics.heightPixels)
                : (int) (adHeight * displayMetrics.density);

        //step4: 请求广告
        AdSlot.Builder builder = new AdSlot.Builder()
                .setCodeId(codeId)
                .setSupportDeepLink(isConfigTrue(config, "deepLink"))
                // 实测发现 要求设置该项, 但都没起作用, 也许是测试模式下不准确, 这里仍然暴露接口, 但给一个默认值
                .setImageAcceptedSize(width, height)
                .setOrientation(
                        isConfigTrue(config, "horizontal")
                                ? TTAdConstant.HORIZONTAL
                                : TTAdConstant.VERTICAL
                )
                .setAdCount(
                        (type == TTadType.FEED || type == TTadType.DRAW) && config.hasKey("count") ?
                                config.getInt("count") : 1
                );
        try {
            switch (type) {
                case FEED:
                    builder.setExpressViewAcceptedSize(adWidth, adHeight);
                    loadTTadFeed(mTTAdNative, builder, hash, false);
                    break;
                case DRAW:
                    // 预加载 自渲染模式 的 draw ad, 该类型默认已不可申请, 但之前做了, 就先保留着
                    if (config.hasKey("native") && config.getBoolean("native")) {
                        loadTTadNativeDraw(mTTAdNative, builder, hash);
                    } else {
                        builder.setExpressViewAcceptedSize(adWidth, adHeight);
                        loadTTadFeed(mTTAdNative, builder, hash, true);
                    }
                    break;
                case FULL:
                    // 当前只有模板渲染了, 所以默认设置为模板渲染, 但指定为 native 仍可使用自渲染
                    if (!config.hasKey("native") || !config.getBoolean("native")) {
                        builder.setExpressViewAcceptedSize(adWidth, adHeight);
                    }
                    loadTTadFull(mTTAdNative, builder, hash);
                    break;
                case REWARD:
                    // 同上
                    if (!config.hasKey("native") || !config.getBoolean("native")) {
                        builder.setExpressViewAcceptedSize(adWidth, adHeight);
                    }
                    loadTTadReward(mTTAdNative, builder, hash, config);
                    break;
                case INTERACTION:
                    builder.setExpressViewAcceptedSize(adWidth, adHeight);
                    loadTTadInter(mTTAdNative, builder, hash);
                    break;
            }
        } catch (Throwable e) {
            // 这里是有可能抛异常的, handle 住
            sendAdEvent(hash, "onVideoError", -104, e.getMessage());
        }
    }

    // 加载 feed draw 信息流广告
    private void loadTTadFeed(TTAdNative mTTAdNative, AdSlot.Builder builder, final String hash, final boolean isDraw) {
        TTAdNative.NativeExpressAdListener listener = new TTAdNative.NativeExpressAdListener() {
            @Override
            public void onError(int code, String message) {
                sendAdEvent(hash, "onVideoError", code, message);
            }

            @Override
            public void onNativeExpressAdLoad(List<TTNativeExpressAd> ads) {
                String uuid;
                WritableArray uuids = Arguments.createArray();
                for (TTNativeExpressAd ad : ads) {
                    uuid = UUID.randomUUID().toString();
                    if (isDraw) {
                        drawAds.put(uuid, ad);
                    } else {
                        feedAds.put(uuid, ad);
                    }
                    uuids.pushString(uuid);
                }
                WritableMap params = Arguments.createMap();
                params.putString("hash", hash);
                params.putString("event", "onVideoLoad");
                params.putArray("uuids", uuids);
                sendEvent(params);
            }
        };
        if (isDraw) {
            mTTAdNative.loadExpressDrawFeedAd(builder.build(), listener);
        } else {
            mTTAdNative.loadNativeExpressAd(builder.build(), listener);
        }
    }

    // 加载 native draw video
    private void loadTTadNativeDraw(TTAdNative mTTAdNative, AdSlot.Builder builder, final String hash) {
        mTTAdNative.loadDrawFeedAd(builder.build(), new TTAdNative.DrawFeedAdListener() {
            @Override
            public void onError(int code, String message) {
                sendAdEvent(hash, "onVideoError", code, message);
            }

            @Override
            public void onDrawFeedAdLoad(List<TTDrawFeedAd> ads) {
                String uuid;
                WritableArray uuids = Arguments.createArray();
                for (TTDrawFeedAd ad : ads) {
                    uuid = UUID.randomUUID().toString();
                    nativeDrawAds.put(uuid, ad);
                    uuids.pushString(uuid);
                }
                WritableMap params = Arguments.createMap();
                params.putString("hash", hash);
                params.putString("event", "onVideoLoad");
                params.putArray("uuids", uuids);
                sendEvent(params);
            }
        });
    }

    // 加载全屏视频广告
    private void loadTTadFull(TTAdNative mTTAdNative, AdSlot.Builder builder, final String hash) {
        mTTAdNative.loadFullScreenVideoAd(builder.build(), new TTAdNative.FullScreenVideoAdListener() {
            @Override
            public void onError(int code, String message) {
                sendAdEvent(hash, "onVideoError", code, message);
            }
            @Override
            public void onFullScreenVideoAdLoad(final TTFullScreenVideoAd ad) {
                mttFullVideoAds.put(hash, ad);
                sendAdEvent(hash, "onVideoLoad", ad.getInteractionType(), null);
            }
            @Override
            public void onFullScreenVideoCached() {
                sendAdEvent(hash, "onVideoCached");
            }
        });
    }

    // 加载激励视频广告
    private void loadTTadReward(TTAdNative mTTAdNative, AdSlot.Builder builder, final String hash, ReadableMap config) {
        builder.setUserID(getConfigStr(config, "userId"));
        builder.setMediaExtra(getConfigStr(config, "extra"));
        builder.setRewardName(getConfigStr(config, "rewardName"));
        builder.setRewardAmount(
                config.hasKey("rewardAmount") ? config.getInt("rewardAmount") : 0
        );
        mTTAdNative.loadRewardVideoAd(builder.build(), new TTAdNative.RewardVideoAdListener() {
            @Override
            public void onError(int code, String message) {
                sendAdEvent(hash, "onVideoError", code, message);
            }
            @Override
            public void onRewardVideoAdLoad(final TTRewardVideoAd ad) {
                mttRewardVideoAds.put(hash, ad);
                sendAdEvent(hash, "onVideoLoad", ad.getInteractionType(), null);
            }
            @Override
            public void onRewardVideoCached() {
                sendAdEvent(hash, "onVideoCached");
            }
        });
    }

    // 加载插屏广告
    private void loadTTadInter(TTAdNative mTTAdNative, AdSlot.Builder builder, final String hash) {
        mTTAdNative.loadInteractionExpressAd(builder.build(), new TTAdNative.NativeExpressAdListener() {
            @Override
            public void onError(int code, String message) {
                sendAdEvent(hash, "onVideoError", code, message);
            }
            @Override
            public void onNativeExpressAdLoad(List<TTNativeExpressAd> ads) {
                if (ads == null || ads.size() == 0){
                    sendAdEvent(hash, "onVideoError", -105, "InteractionExpressAd response empty");
                } else {
                    initTTadInter(hash, ads.get(0));
                }
            }
        });
    }

    // 插屏广告逻辑不太一样, 为了更好的用户体验, render 完成后再发送 onVideoLoad
    private void initTTadInter(final String hash, final TTNativeExpressAd ad) {
        ad.setExpressInteractionListener(new TTNativeExpressAd.AdInteractionListener() {
            @Override
            public void onAdShow(View view, int type) {
                // 仅触发一次, 且为了防止 InteractionType 有变动, 在 onShow 时也传递一下 type
                lastClickInteraction = null;
                if (lastInteractionDismiss) {
                    lastInteractionDismiss = false;
                    if (isConfigInterTrue(hash, "onVideoShow")) {
                        sendAdEvent(hash, "onVideoShow", type, null);
                    }
                }
            }

            @Override
            public void onAdClicked(View view, int type) {
                lastClickInteraction = hash;
                if (isConfigInterTrue(hash, "onVideoClick")) {
                    sendAdEvent(hash, "onVideoClick");
                }
            }

            @Override
            public void onAdDismiss() {
                if (!lastInteractionDismiss) {
                    lastInteractionDismiss = true;
                }
                // 由 AdClicked 引起的 插屏关闭, 不触发 skip 事件
                if (lastActiveInteraction != null) {
                    return;
                }
                // 这里是关闭了 插屏广告, 触发 onVideoSkipped, 即跳过了广告
                if (isConfigInterTrue(hash, "onVideoSkip")) {
                    sendAdEvent(hash, "onVideoSkip");
                }
                mttInteractionConfigs.remove(hash);
            }

            @Override
            public void onRenderFail(View view, String msg, int code) {
                // 这里先不发送, 而是缓存起来, 待调用 showInteraction 时在发送
                mttInteractionErrs.put(hash, new String[]{String.valueOf(code), msg});
            }

            @Override
            public void onRenderSuccess(View view, float width, float height) {
                mttInteractionAds.put(hash, ad);
                sendAdEvent(hash, "onVideoLoad", ad.getInteractionType(), width + "_" + height);
            }
        });
        ad.render();
    }

    @Override
    public void onHostDestroy() {
    }

    @Override
    public void onHostPause() {
        // 当前 activity 触发 onHostPause, 判断是否由于点击插屏引起的
        if (lastClickInteraction != null) {
            lastActiveInteraction = lastClickInteraction;
            lastClickInteraction = null;
        }
    }

    /**
     * 在这里 触发插屏广告的 onVideoClose 事件
     */
    @Override
    public void onHostResume() {
        // 当前 activity 触发 onHostResume, 检查是否为 关闭插屏广告页 触发的
        if (lastActiveInteraction == null) {
            return;
        }
        // 关闭了插屏广告页, 但 插屏 并未关闭, 也不做处理
        if (!lastInteractionDismiss) {
            lastActiveInteraction = null;
            return;
        }
        // 关闭 插屏广告页, 且 插屏也已关闭, 触发videoClose
        if (isConfigInterTrue(lastActiveInteraction, "onVideoClose")) {
            sendAdEvent(lastActiveInteraction, "onVideoClose");
        }
        mttInteractionConfigs.remove(lastActiveInteraction);
        lastActiveInteraction = null;
    }

    // 下载监听器, 如果点开广告后 点击下载, 然后关了广告, 那么下载事件就停了, 应该是进程退出了
    private TTAppDownloadListener getDownloadListener(final String hash, final ReadableMap config) {
        final boolean onIdle = isConfigTrue(config, "onIdle");
        final boolean onDownloadProgress = isConfigTrue(config, "onDownloadProgress");
        final boolean onDownloadPaused = isConfigTrue(config, "onDownloadPaused");
        final boolean onDownloadFailed = isConfigTrue(config, "onDownloadFailed");
        final boolean onDownloadFinished = isConfigTrue(config, "onDownloadFinished");
        final boolean onInstalled = isConfigTrue(config, "onInstalled");
        if (
                !onIdle && !onDownloadProgress && !onDownloadPaused &&
                !onDownloadFailed && !onDownloadFinished && !onInstalled
        ) {
            return null;
        }
        return new TTAppDownloadListener() {
            @Override
            public void onIdle() {
                if (onIdle) {
                    sendDownloadEvent(hash, "onIdle", null, null, -1, -1);
                }
            }

            @Override
            public void onDownloadActive(long totalBytes, long currBytes, String fileName, String appName) {
                if (onDownloadProgress) {
                    sendDownloadEvent(hash, "onDownloadProgress", fileName, appName, totalBytes, currBytes);
                }
            }

            @Override
            public void onDownloadPaused(long totalBytes, long currBytes, String fileName, String appName) {
                if (onDownloadPaused) {
                    sendDownloadEvent(hash, "onDownloadPaused", fileName, appName, totalBytes, currBytes);
                }
            }

            @Override
            public void onDownloadFailed(long totalBytes, long currBytes, String fileName, String appName) {
                if (onDownloadFailed) {
                    sendDownloadEvent(hash, "onDownloadFailed", fileName, appName, totalBytes, currBytes);
                }
            }

            @Override
            public void onDownloadFinished(long totalBytes, String fileName, String appName) {
                if (onDownloadFinished) {
                    sendDownloadEvent(hash, "onDownloadFinished", fileName, appName, totalBytes, -1);
                }
            }

            @Override
            public void onInstalled(String fileName, String appName) {
                if (onInstalled) {
                    sendDownloadEvent(hash, "onInstalled", fileName, appName, -1, -1);
                }
            }
        };
    }

    // 插屏请求配置是否为 true
    private boolean isConfigInterTrue(String hash, String key) {
        ReadableMap config = mttInteractionConfigs.containsKey(hash)
                ? mttInteractionConfigs.get(hash) : null;
        return config != null && isConfigTrue(config, key);
    }

    private boolean isConfigTrue(ReadableMap config, String key) {
        return config.hasKey(key) && config.getBoolean(key);
    }

    private String getConfigStr(ReadableMap config, String key) {
        return config.hasKey(key) ? config.getString(key) : null;
    }

    private void sendAdEvent(String hash, String event) {
        sendAdEvent(hash, event, 0, null);
    }

    private void sendAdEvent(String hash, String event, int code, @Nullable String error) {
        WritableMap params = Arguments.createMap();
        params.putString("hash", hash);
        params.putString("event", event);
        params.putInt("code", code);
        params.putString("error", error);
        sendEvent(params);
    }

    private void sendDownloadEvent(
            String hash,
            String event,
            String fileName,
            String appName,
            long totalBytes,
            long currBytes
    ) {
        WritableMap params = Arguments.createMap();
        params.putString("hash", hash);
        params.putString("event", event);
        params.putString("fileName", fileName);
        params.putString("appName", appName);
        params.putDouble("totalBytes", totalBytes);
        params.putDouble("currBytes", currBytes);
        sendEvent(params);
    }

    private void sendEvent(WritableMap params) {
        if (mJSModule == null) {
            mJSModule = reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
        }
        mJSModule.emit("TTadEvent", params);
    }
}
