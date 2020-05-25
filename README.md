# react-native-ttad
react native ttad

# 安装

`yarn add react-native-ttad`


# Android 配置

## 1.`android/app/src/main/androidManifest.xml`

```
<manifest ..>

    <!--必须要有的权限-->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <!--可选权限-->
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>
    <uses-permission android:name="android.permission.GET_TASKS"/>

    <!--最好能提供的权限, 提升广告投放精准度-->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />


    <application>

        <!--添加本段, Android 7.0 以上需要-->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.TTFileProvider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>


    </application>
</manifest>
```

## 2.新建 `android/app/src/main/res/xml/file_paths.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-path name="tt_external_root" path="." />
    <external-path name="tt_external_download" path="Download" />
    <external-files-path name="tt_external_files_download" path="Download" />
    <files-path name="tt_internal_file_download" path="Download" />
    <cache-path name="tt_internal_cache_download" path="Download" />
</paths>
```


## 3.依赖冲突

若编译过程，相关依赖版本冲突，请参阅 [build.gradle](android/build.gradle#L32)


# iOS 配置

还未开发 iOS 版本，TODO



# 使用

``` js
import {
    // Api
    initSdk,
    fullVideo, //全屏视频
    lastFullVideo,
    rewardVideo, //激励视频
    lastRewardVideo,
    interaction, //插屏
    lastInteraction,
    loadFeed,
    loadDraw,

    // 组件
    TTadBanner, 
    TTadFeed, 
    TTAdInteraction, 
    TTAdSplash, 
    TTAdDraw
} from 'react-native-ttad'


// 也可以
import ttad, {TTadBanner} from 'react-native-ttad'

// 调用api
ttad.initSdk();
```


## initSdk

初始化 sdk，在项目入口处调用

``` js
initSdk({
    appId:"", // 应用ID, 该项必须(其他可选)
    appName:"", //应用名称, 默认自动获取， 可另设
    showNotify:Bool, //是否允许sdk显示通知栏, 默认 true
    download4g:Bool, //在4g网络下是否可直接下载app, 默认 false
    lightBar:Bool, //打开广告网页是否使用浅色标题栏, 默认 false
    debug:Bool,   //是否输出日志 (是 android 日志,不是js), debug模式默认为true, 反之false
})
```

## API 

```js
// onLoad / onCached 回调参数与 load() 返回的是同一个对象

Player = [fullVideo|rewardVideo|interaction](codeId) // 设置广告ID

    .horizontal(Bool) //是否获取横屏视频, 默认 false
    .deepLink(Bool)   //是否允许 deepLink, 默认 false
    .permission(Bool)  //是否自动索要权限, 默认 false, 不建议自动, 而是在加载广告前更友好的询问

    .size(width, height) //设置广告尺寸 可仅设置 width (高度自动), 也可二者都设置
                         //一般只用于插屏, 全屏/激励适配虽可以设置, 但没什么作用

    .isNative(false)    //是否为自渲染广告（支持 fullVideo / rewardVideo），
                        //默认已无法创建该类型广告了, 新创建的只有模板渲染类型  

    // 激励视频专用, 奖励配置, 具体请参考官方文档, 还可能牵涉服务端通信
    .userId(String)
    .rewardName(String)
    .rewardAmount(String)
    .extra(String)

    .onLoad(player => {}) // 加载成功回调
    .onError(error => {}) // 加载失败回调
    .onCached(player => {}) // 缓存成功回调 (插屏无此回调)

    .load();  // 开始加载


// 获取最后一次加载的对象
Player = [lastFullVideo|lastRewardVideo|lastInteraction]();


// 显示广告
player
    .ritScenes(Int) // 设置场景 0~11
    .scenes(String) // ritScenes=11 时为自定义场景, 手动设置字符串
    .showDownLoadBar(Bool) //是否显示下载条(广告为下载类时生效)

    .onError(callback) //发生错误回调
    .onShow(callback) //显示后回调
    .onClick(callback) //点击回调
    .onSkip(callback) //跳过回调 (全屏视频跳过|插屏关闭)
                      //因为用户点击广告导致的关闭, 而非主动关闭插屏, 不触发
    .onComplete(callback) //视频播放完成后回调
    .onReward(callback) //激励视频,需发放奖励回调
    .onClose(callback) //广告关闭回调 , 对于插屏, 只有在插屏关闭 + 弹窗关闭才触发

    .onIdle(callback) //下载空闲
    .onDownloadProgress(callback) //下载进度
    .onDownloadPaused(callback) //下载暂停
    .onDownloadFailed(callback) //下载失败
    .onDownloadFinished(callback) //下载完成
    .onInstalled(callback) //安装完成

    .show(); //显示


// player 可以使用 load() 的返回, 或 onLoad | onCached 回调参数
// 或者使用 last*** 获取，但以下接口只能在 onLoad | onCached 回调中使用

//广告类型 - 0: 全屏视频, 1:激励视频, 2:插屏
(int) player.type() 

// 广告展示类型 
// 2: 浏览器内打开 （普通类型）
// 3: 落地页（普通类型)
// 4: 应用下载
// 5: 拨打电话 
// -1:未知类型
(int) type = player.interaction() 

// 获取广告尺寸, 仅针对插屏
{(int) width, (int) height} = player.size


// 最后，对于使用 last*** 系列获取到的 player，可在确认加载完成后显示
if (player.canplay()) {
    player.show()
}

```

备注：关于场景值，可参见 [mttRitScenes](android/src/main/java/com/malacca/ttad/TTadModule.java#L45)



## 组件

组件必须要有 width 尺寸，可以是通过 style 指定的，也可以是 flex 布局从父级继承的；
TTAdSplash / TTAdDraw 除 width 外，还必须要有 height，也可以是直接指定或从父级继承的

```jsx

// 通用属性
<Componet
    codeId=""   //广告ID
    deepLink={false} //是否支持 deepLink
    listener={callback} //相关回调
/>

// Banner
<TTadBanner
    intervalTime={0}   //轮播间隔时长, 毫秒
    canInterrupt={false} //若广告是视频, 是否可以暂停
    dislikeDisable={false} //禁用不喜欢, 还是会显示关闭那个x, 点了没效果
    dislikeNative={false} //点击不喜欢，是否弹出原生菜单 (是：菜单在底部, 否：菜单在中间)
    handleDislike={false} //是否自行处理不喜欢回调, 默认处理方式为移除组件
/>

// feed 信息流
<TTadFeed
    canInterrupt={false} 
    dislikeDisable={false}
    dislikeNative={false} 
    handleDislike={false}
/>

// 插屏
<TTAdInteraction
    canInterrupt={false} 
/>

// 启动屏
<TTAdSplash
    timeout={3000} //加载超时的时长(毫秒)
/>

// draw 信息流
<TTAdDraw
    canInterrupt={false} 
    native={false} // 是否为自渲染 draw video, 该类型默认已无法申请
/>
```

组件的 listeners 需要指定为一个函数，可监听广告的各种回调

具体可参见： [Helper.js](src/Helper.js)

```
listener = {bus => {
    bus
    .onLoad()
    .onFail()
    .onShow()
    ....
}}
```

# 预加载

对于 TTadFeed / TTAdDraw 组件，一般用于列表，可进行预加载

```
[loadFeed|loadDraw](codeId, count)  //广告ID, 预加载条数
    .horizontal(Bool) 
    .deepLink(Bool)   
    .permission(Bool)  

    .size(width, height) // 广告尺寸, 建议设置
    .isNative(false)    // loadDraw 是否为预加载自渲染 draw video

    .onError(err => {})
    .onLoad(uuids => {

        // 获取 uuids 为数组  ["xxx", "yyyy"]

    })
    .load()
```

通过预加载得到的 uuid 载入组件


``` js
// 与正常组件相同, 只需将 codeId 替换为 uuid

<TTadFeed || TTAdDraw
    uuid=""  //设置为 预加载得到的 uuid
    canInterrupt={false} 
    native={false}
/>
```
