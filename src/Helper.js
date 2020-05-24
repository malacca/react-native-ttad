
const bindBasic = [
  '_onLoad',  // 加载成功
  '_onFail',  // 加载失败 或 其他错误
];

const bindClick = [
  '_onShow',  // 广告显示了
  '_onClick',  // 用户点了广告
  '_onOpen',  // 若广告为网页类型, 可能会触发这个事件
  '_onClose',  // 广告关闭了
];

const bindSplash = [
  '_onSkip', // 跳过广告
  '_onTimeOver', // 广告播放完毕
];

// 若广告是下载 app 类型的, 可监听下载, 全适用
const bindDownload = [
  '_onIdle',  // 下载空闲
  '_onDownloadProgress', //下载进度
  '_onDownloadPaused', //下载暂停
  '_onDownloadFailed', //下载失败
  '_onDownloadFinished', //下载完成
  '_onInstalled', //安装完成
];

// 对于视频广告的监听, 开屏(splash) 不适用，其他都可以
const bindVideo = [
  '_onVideoLoad',  //加载成功
  '_onVideoError', //加载失败
  '_onVideoPlay',  //开始播放
  '_onVideoPaused', //播放暂停
  '_onVideoContinue', //播放继续
  '_onVideoProgress', //播放进度
  '_onVideoComplete', //播放完成
];

const bindSpeical = [
  '_onVideoRetry', //视频类型广告重新播放
  '_onTimeout', // 开屏广告加载超时
  '_onDislike', // 点击了不喜欢
  '_onDrawClick', // draw 类型创意元素被点击
];


// 绑定类, 可使用链式风格绑定事件, 如 bus.onClick(() => {}).onShow(() => {})...
class Bus {}
bindBasic.concat(bindClick, bindSplash, bindDownload, bindVideo, bindSpeical).forEach(k => {
  Object.defineProperty(Bus.prototype, k.substr(1), {
    value:function(callback) {
      this[k] = callback;
      return this
    }
  })
})

// 获取自定义的监听参数, 这部分逻辑需与原生模块配合
const getEvent = (obj, type, disableCard) => {
  const event = {};
  for (let k in obj) {
    event[k] = true;
  }

  // 根据绑定的事件 让原生端根据需要 绑定 click/video/download 事件集
  if (bindDownload.some(e => e in event)) {
    event._bindDownload = true;
  }

  const isNativeDraw = type === 'draw_native';
  const isSplash = !isNativeDraw && type === 'splash';

  // nativeDraw splash 类型 click 事件为动态绑定
  const listenClick = isNativeDraw || isSplash ? bindClick.some(e => e in event) : false;

  if (!isSplash) {
    // nativeDraw 类型使用默认 card, 必须 bindVideo, 因为默认的 card 需要监听反馈
    let listenVideo = (isNativeDraw && !disableCard) || bindVideo.some(e => e in event);

    // nativeDraw 类型的 retry 在 click 事件集上 / express 类型无需动态绑定 click 事件集, 原生端总会通知
    const retry = isNativeDraw || !listenVideo ? '_onVideoRetry' in event : false;

    if (!isNativeDraw) {
      listenVideo = listenVideo || retry;
    } else if (retry || listenClick){
      event._bindClick = true;
    }
    if (listenVideo) {
      event._bindVideo = true;
      // nativeDraw 使用默认 card, 原生端必须触发以下事件
      if (isNativeDraw && !disableCard) {
        event._onVideoPlay = true;
        event._onVideoComplete = true;
      }
    }
  } else if (listenClick || bindSplash.some(e => e in event)) {
    // splash 类型如果有 skip timeOver 事件, 也要绑定 click 事件
    event._bindClick = true;
  }

  const binds = {};
  for (let k in event) {
    binds[k.substr(1)] = true;
  }
  return binds;
}

// 返回支持的 props
const makeProps = (nativeProps, type) => {
  const {children, deepLink, listener, disableCard, ...props} = nativeProps;
  props.deepLink = !!deepLink;

  // 获取要传递给原生端的 绑定参数
  let bus;
  if (listener && typeof listener === 'function') {
    bus = new Bus();
    listener(bus);
    props.listeners = getEvent(bus, type, disableCard);
  }
  return {bus, props};
};

// 处理接收到的消息
const reciveEvent = (ref, bus, e) => {
  let {event, code, error, ...message} = e.nativeEvent;
  if (event === "onLoad") {
    const {update, height} = message;
    if (update) {
      ref.setNativeProps({height})
    }
  } else if (event === 'onFail') {
    message = {code, error};
  }
  const key = '_' + event;
  bus && bus[key] && bus[key](message);
  return event;
}

export {
  makeProps,
  reciveEvent
};
