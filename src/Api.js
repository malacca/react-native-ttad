import {Platform, NativeModules, DeviceEventEmitter, NativeAppEventEmitter} from 'react-native';
const { TTadModule } = NativeModules, IsAndroid = Platform.OS === 'android';

let autoHashIndex = 0;
const Listeners = {};
const gint = (v) => {
    v = parseInt(v);
    return typeof v === "number" && isFinite(v) && Math.floor(v) === v ? v : 0;
};

/**
 * 显示加载好的 全屏/激励/插屏 广告
 */
class Player {
    _canplay = false;
    _showed = false;
    _eagerShow = null;
    _hash = null;
    _type = null;
    _interaction = false;
    _size = null;
    _ritScenes = 0;
    _scenes = null;

    //通用
    _onShow = null;  // 广告显示了
    _onClick = null;  // 用户点了广告
    _onClose = null;  // 广告关闭了

    // 全屏|激励视频 可用
    _onSkip = null;  // 广告跳过了
    _onComplete = null;  // 广告播完了
    _onError = null;  // 播放失败

    // 激励视频 可用
    _onReward = null; // 激励视频结果

    // app 下载器监听
    _showDownLoadBar = false; //是否显示进度条
    _onIdle = null; // 下载空闲
    _onDownloadProgress = null; //下载进度
    _onDownloadPaused = null; //下载暂停
    _onDownloadFailed = null; //下载失败
    _onDownloadFinished = null; //下载完成
    _onInstalled = null; //安装完成

    constructor(hash, type) {
        this._hash = hash;
        this._type = type;
    }

    // 广告信息获取
    type() {
        // 当前广告展示类型 
        // 0: 全屏视频, 1:激励视频, 2:插屏
        return this._type;
    }
    interaction() {
        // 当前广告展示类型 
        // 2: 浏览器内打开 （普通类型）
        // 3: 落地页（普通类型)
        // 4: 应用下载
        // 5: 拨打电话 
        // -1:未知类型
        return this._interaction;
    }
    canplay(){
        // 是否已成功加载, 可以显示了
        return this._canplay;
    }
    size(){
        // 插屏可用 获取当前广告尺寸
        return this._size;
    }

    //以下是在 show() 之前进行设定
    onShow(callback) {
        this._onShow = callback;
        return this
    }
    onClick(callback) {
        this._onClick = callback;
        return this
    }
    onSkip(callback) {
        this._onSkip = callback;
        return this
    }
    onClose(callback) {
        this._onClose = callback;
        return this
    }

    // 全屏|激励视频 可用
    ritScenes(rit){
        // 设置场景
        this._ritScenes = Math.max(0,  Math.min(11, gint(rit)) );
        return this;
    }
    scenes(v){
        // 若场景为自定义场景, 自行设置一个字符串
        this._scenes = v;
        return this;
    }
    onComplete(callback) {
        this._onComplete = callback;
        return this
    }
    onError(callback) {
        this._onError = callback;
        return this
    }

    // 激励视频 可用
    onReward(callback) {
        this._onReward = callback;
        return this;
    }

    // app 下载器监听
    showDownLoadBar(v){
        this._showDownLoadBar = v === undefined ? true : Boolean(v);
        return this
    }
    onIdle(callback){
        this._onIdle = callback;
        return this;
    }
    onDownloadProgress(callback){
        this._onDownloadProgress = callback;
        return this;
    }
    onDownloadPaused(callback){
        this._onDownloadPaused = callback;
        return this;
    }
    onDownloadFailed(callback){
        this._onDownloadFailed = callback;
        return this;
    }
    onDownloadFinished(callback){
        this._onDownloadFinished = callback;
        return this;
    }
    onInstalled(callback){
        this._onInstalled = callback;
        return this;
    }

    show(ritScenes, scenes) {
        // 可能是调用加载后立即 show(), 此时广告还没加载上, 打个点
        if (!this._canplay) {
            this._eagerShow = [ritScenes, scenes];
            return;
        }
        // 广告一旦 show 过, 就不能再次用了
        if (this._showed) {
            this._onError && this._onError({
                code: -200,
                error: 'ad can only shown once'
            })
            return;
        }
        this._showed = true;
        const config = {
            onVideoShow: this._onShow != null,
            onVideoClick: this._onClick != null,
            onVideoSkip: this._onSkip != null,
            onVideoClose: this._onClose != null,
            
            onIdle: this._onIdle != null,
            onDownloadProgress: this._onDownloadProgress != null,
            onDownloadPaused: this._onDownloadPaused != null,
            onDownloadFailed: this._onDownloadFailed != null,
            onDownloadFinished: this._onDownloadFinished != null,
            onInstalled: this._onInstalled != null
        };
        // 显示插屏广告
        if (this._type > 1) {
            TTadModule.showInteraction(this._hash, config);
            return;
        }
        if (ritScenes === undefined) {
            ritScenes = this._ritScenes;
        }
        if (scenes === undefined) {
            scenes = this._scenes
        }
        config.showDownLoadBar = this._showDownLoadBar;
        config.onVideoComplete = this._onComplete != null;
        if (this._type > 0) {
            config.onRewardVerify = this._onReward != null;
            TTadModule.showRewardVideo(this._hash, config, ritScenes, scenes)
        } else {
            TTadModule.showFullVideo(this._hash, config, ritScenes, scenes)
        }
        return this;
    }
    _showEager() {
        // 如果曾使用 lastVideo() 方法调用 show()
        // 在 video 实际载入后会 异步再次 触发 show() -> 真播放
        if (this._eagerShow !== null) {
            this.show(this._eagerShow[0], this._eagerShow[1]);
            this._eagerShow = null;
        }
    }
}


/**
 * 用于加载 全屏/激励/插屏 广告
 * 也用来 预加载 feed/draw 广告
 */
class Loader {
    _hash = null;
    _type = 0;
    _codeId = null;
    _horizontal = false;
    _deepLink = false;
    _permission = false;
    _native = false;
    _width = 0;
    _height = 0;

    // 加载条数 (feed draw 类型专用)
    _count = 1;

    // 激励视频专用
    _userId = null;
    _extra = null;
    _rewardName = null;
    _rewardAmount = 0;

    _onError = null; // 加载错误
    _onLoad = null;  // 加载完成
    _onCached = null;  // 缓存成功
    _player = null; // 播放器

    constructor(hash, codeId, type) {
        this._hash = hash;
        this._codeId = codeId;
        this._type = type;
    }
    horizontal(v) {
        this._horizontal = v === undefined ? true : Boolean(v);
        return this
    }
    deepLink(v) {
        this._deepLink = v === undefined ? true : Boolean(v);
        return this
    }
    permission(v) {
        this._permission = v === undefined ? true : Boolean(v);
        return this
    }

    // 尺寸, 可缺省, 
    // 1. 插屏、信息流、draw 可设置宽度(高度自适应), 也可以宽高都设置
    // 2. 全屏/激励视频也可以设置, 但没什么实际作用
    size(width, height){
        this._width = gint(width);
        this._height = gint(height);
        return this;
    }

    // 全屏 / 激励 / draw  是否为自渲染模式, 该类型默认已无法申请
    isNative(v) {
        this._native = v;
        return this;
    }

    // 激励视频 设置激励参数
    userId(v){
        this._userId = v;
        return this;
    }
    extra(v){
        this._extra = v;
        return this;
    }
    rewardName(v){
        this._rewardName = v;
        return this;
    }
    rewardAmount(v) {
        this._rewardAmount = gint(v);
        return this;
    }
    // 绑定回调
    onError(callback) {
        this._onError = callback;
        return this
    }
    onLoad(callback) {
        this._onLoad = callback;
        return this
    }
    onCached(callback) {
        this._onCached = callback;
        return this
    }
    load(){
        const config = {
            hash: this._hash,
            codeId: this._codeId,
            horizontal: this._horizontal,
            deepLink: this._deepLink,
            permission: this._permission,
            width: this._width,
            height: this._height
        }
        // draw 类型
        if (this._type > 3) {
            config.count = this._count;
            config.native = this._native;
            TTadModule.loadDraw(config);
            return this;
        }
        // feed 类型
        if (this._type > 2) {
            config.count = this._count;
            TTadModule.loadFeed(config);
            return this;
        }

        // 直接展示的广告类型
        if (this._player !== null) {
            return this._player;
        }
        this._player = new Player(this._hash, this._type);
        if (this._type > 1) {
            // 插屏
            TTadModule.loadInteraction(config);
        } else if (this._type > 0) {
            // 激励视频
            config.userId = this._userId;
            config.extra = this._extra;
            config.rewardName = this._rewardName;
            config.rewardAmount = this._rewardAmount;
            config.native = this._native;
            TTadModule.loadRewardVideo(config);
        } else {
            // 全屏视频
            config.native = this._native;
            TTadModule.loadFullVideo(config);
        }
        return this._player;
    }
}

// 加载广告
const loadTTAd = (codeId, type) => {
    const hash = 's' + (++autoHashIndex);
    const loader = new Loader(hash, codeId, type);
    return Listeners[hash] = loader;
}

// 获取最后请求的广告
const lastTTAd = (type) => {
    let player = undefined;
    const keys = Object.keys(Listeners), len = keys.length;
    if (len) {
        type = gint(type);
        keys.reverse().some(hash => {
            if (Listeners[hash] && Listeners[hash]._type === type) {
                player = Listeners[hash];
                return true;
            }
        });
    }
    return player
}

// 监听消息
const listenTTadEvent = msg => {
    const {hash, event, ...message} = msg;
    if (!hash || !(hash in Listeners)) {
        return;
    }
    const listener = Listeners[hash];
    const type = listener._type;
    const player = listener._player;
    if (type < 3 && !player) {
        return;
    }
    const {code, error, ...props} = message;
    
    // 加载视频
    if (event === 'onVideoError') {
        listener._onError && listener._onError({code, error});
        // 如果是不是信息流类型的, 这里给 player 也触发一下
        if (type < 3) {
            player._onError && player._onError({code, error});
        }
        // 加载就发生错误, 其他事件也不会被触发了, 移除
        delete Listeners[hash];
        return;
    }
    
    // 加载成功
    if (event === 'onVideoLoad') {
        if (type < 3) {
            player._canplay = true;
            player._interaction = code;
            // 插屏广告, 这里有一个尺寸顺带通过 error 传出来了
            if (type > 1 && error && error.indexOf('_') > -1) {
                const size = error.split('_');
                player._size = {
                    width: gint(size[0]),
                    height: gint(size[1])
                }
            }
            listener._onLoad && listener._onLoad(player);
            player._showEager();
        } else {
            const {uuids} = props;
            if (Array.isArray(uuids)) {
                listener._onLoad && listener._onLoad(uuids);
            } else {
                player._onError && player._onError({code:-200, error:'get uuids failed'});
            }
            // 信息流, 拿到 uuids 后 移除缓存
            delete Listeners[hash];
        }
        return;
    }

    // 理论上 type>2 就不会有以下触发的, 这里仅做个防御性 return
    if (type > 2) {
        return;
    }
    switch (event) {
        // 全屏视频/激励视频 还会有一个缓存成功的回调
        case 'onVideoCached':
            player._canplay = true;
            listener._onCached && listener._onCached(player);
            player._showEager();
            break;

        // 加载成功, 但无法渲染, 其他事件不会被触发了, 移除缓存
        case 'onVideoUnPlay':
            player._onError && player._onError({code, error});
            delete Listeners[hash];
            break;

        // 对于插屏 onVideoShow 也会传处理一个 _interaction, 为防止有变动, 这里也同步更新一下
        case 'onVideoShow':
            if (type > 1 && code !== undefined) {
                player._interaction = code;
            }
            player._onShow && player._onShow();
            break;

        // 对于插屏而言, skipp 之后就不会有其他事件了, 移除缓存
        case 'onVideoSkip':
            player._onSkip && player._onSkip();
            if (type > 1) {
                delete Listeners[hash];
            }
            break;

        // 加载的广告只能用一次, 既然 close 了, 移除缓存
        case 'onVideoClose':
            player._onClose && player._onClose();
            delete Listeners[hash];
            break;

        // 广告被点击
        case 'onVideoClick':
            player._onClick && player._onClick();
            break;

        // 广告播放完毕
        case 'onVideoComplete':
            player._onComplete && player._onComplete();
            break;

        // 激励视频 可以给奖励了
        case 'onRewardVerify':
            player._onReward && player._onReward(props);
            break;

        // 下载空闲
        case 'onIdle':
            player._onIdle && player._onIdle();
            break;

        // 下载中
        case 'onDownloadProgress':
            player._onDownloadProgress && player._onDownloadProgress(props);
            break;

        // 下载暂停
        case 'onDownloadPaused':
            player._onDownloadPaused && player._onDownloadPaused(props);
            break;

        // 下载失败
        case 'onDownloadFailed':
            player._onDownloadFailed && player._onDownloadFailed(props);
            break;

        // 下载完成
        case 'onDownloadFinished':
            player._onDownloadFinished && player._onDownloadFinished(props);
            break;

        // 安装成功
        case 'onInstalled':
            player._onInstalled && player._onInstalled(props);
            break;

        default:
            break;
    }
}
if(IsAndroid) {
    DeviceEventEmitter.addListener("TTadEvent", listenTTadEvent);
} else {
    NativeAppEventEmitter.addListener("TTadEvent", listenTTadEvent);
}

// 载入 sdk
const initSdk = (config) => {
    TTadModule.initSdk(config||{});
}

// 全屏视频广告
const fullVideo = (codeId) => {
    return loadTTAd(codeId, 0);
}
const lastFullVideo = () => {
    return lastTTAd(0);
}

// 激励视频广告
const rewardVideo = (codeId) => {
    return loadTTAd(codeId, 1);
}
const lastRewardVideo = () => {
    return lastTTAd(1);
}

// 插屏广告
const interaction = (codeId) => {
    return loadTTAd(codeId, 2);
}
const lastInteraction = () => {
    return lastTTAd(2);
}

// 预加载信息流
const loadFeedAd = (type, codeId, count) => {
    const loader = loadTTAd(codeId, type);
    loader._count = Math.max(1,  gint(count));
    return loader;
}
const loadFeed = (codeId, count) => {
    return loadFeedAd(3, codeId, count);
}
const loadDraw = (codeId, count) => {
    return loadFeedAd(4, codeId, count);
}

export default {
    initSdk,
    fullVideo,
    lastFullVideo,
    rewardVideo,
    lastRewardVideo,
    interaction,
    lastInteraction,
    loadFeed,
    loadDraw
};
