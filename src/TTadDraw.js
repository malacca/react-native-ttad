import React, { PureComponent } from 'react';
import {
  requireNativeComponent, 
  UIManager,
  findNodeHandle, 
  StyleSheet, 
  TouchableOpacity, 
  Animated,
  View, 
  Image, 
  Text
} from 'react-native';
import {makeProps, reciveEvent} from './Helper';

const RNTTDrawView = requireNativeComponent('RNTTDrawView');
const RNTTDrawNativeView = requireNativeComponent('RNTTDrawNativeView');

// 穿山甲logo
let _TTadLogoBase64 = null;

class TTAdDraw extends PureComponent {
  constructor(props) {
    super(props);
    if (this._isNative = Boolean(this.props.native)) {
      this._initNative();
    }
  }


  // 自渲染 draw
  //-------------------------------------
  _initNative = () => {
    this.state = {
      logo:null,
      title:null,
      description:null,
      buttonText:null,
      icon: {},
    };
    this._cardOpacity = null;
    this._cardFadeIn = null;
    this._cardFadeOut = null;
    this._createAnimation = () => {
      if (this._cardOpacity !== null) {
        return;
      }
      this._cardOpacity = new Animated.Value(0);
      this._cardFadeIn = Animated.timing(this._cardOpacity, {
        toValue:1,
        duration:2500,
        useNativeDriver:true,
        isInteraction:false, 
      });
      this._cardFadeOut = Animated.timing(this._cardOpacity, {
        toValue:0,
        duration:1500,
        useNativeDriver:true,
        isInteraction:false, 
      });
    }
  }

  _fadeInOut = (out) => {
    this._cardFadeIn.stop();
    this._cardFadeOut.stop();
    if (out) {
      this._cardFadeOut.start();
    } else {
      this._cardFadeIn.start();
    }
  }

  _emit = (bus, event, message) => {
    const key = '_' + event;
    bus && bus[key] && bus[key](message);
  }

  _eventNative = (e) => {
    let {event, code, error, logo, ...message} = e.nativeEvent;
    if (event === 'onLoad') {
      if (_TTadLogoBase64) {
        message.logo = _TTadLogoBase64;
      } else {
        _TTadLogoBase64 = message.logo = 'data:image/png;base64,' + logo;
      }
    } else if (event === 'onFail') {
      message = {code, error};
    }

    // 禁用了默认 card, 仅通知即可
    const {disableCard} = this.props;
    if (disableCard) {
      return this._emit(this._bus, event, message);
    }

    // 处理默认 card
    if (event === 'onLoad') {
      const {logo, title, description, buttonText, icon} = message;
      this.setState({logo, title, description, buttonText, icon})
    } else if (event === 'onVideoPlay') {
      this._fadeInOut();
    } else if (event === 'onVideoComplete') {
      this._fadeInOut(true);
    }
    this._emit(this._bus, event, message);
  }

  _renderCard() {
    if (this.state.title === null) {
      return null;
    }
    const {url} = this.state.icon||{};
    const icon = url ? <Image source={{uri:url}} style={styles.icon}/> : null;
    const logo = <Image source={{uri:this.state.logo}} style={styles.logo}/>
    return <Animated.View style={[styles.card, {opacity: this._cardOpacity}]}>
        <View style={styles.title}>
          {icon}
          <Text style={styles.titleText}>{this.state.title}</Text>
          <TouchableOpacity style={styles.button} activeOpacity={0.9} onPress={this.open}>
            <Text style={styles.buttonText}>{this.state.buttonText}</Text>
          </TouchableOpacity>
        </View>
        <Text style={styles.description}>{logo} {this.state.description}</Text>
    </Animated.View>
  }

  _renderNativeDrawView(nativeProps){
    // logo 仅请求一次, 缓存起来
    const {needAdLogo, ...props} = nativeProps;
    if (!_TTadLogoBase64) {
      props.needAdLogo = true;
    }
    return (
      <RNTTDrawNativeView
        {...props}
        ref="ad"
        onTTadViewEvent={this._eventNative}
      />
    );
  }
 
  _renderNativeDraw() {
    const {listener, style, ...nativeProps} = this.props;
    const {disableCard} = nativeProps;

    if (listener) {
      nativeProps.listener = listener;
    } else if (!disableCard) {
      // 使用默认 card, 必须有 listener
      nativeProps.listener = () => {};
    }

    const {bus, props} = makeProps(nativeProps, 'draw_native')
    this._bus = bus;

    // 不使用默认card, 只显示视频
    if (disableCard) {
      props.style = style;
      return this._renderNativeDrawView(props);
    }
    this._createAnimation();
    props.style = StyleSheet.absoluteFill;
    return <View style={style}>{this._renderNativeDrawView(props)}{this._renderCard()}</View>
  }

  // 使用 ref.open() 触发 自渲染 draw 广告的点击事件
  open = () => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this.refs.ad),
      "clickDraw", 
      []
    );
  }


  // 模板渲染的 draw
  //-------------------------------------
  _event = (e) => {
    reciveEvent(this.refs.ad, this._bus, e);
  }

  render(){
    if (this._isNative) {
      return this._renderNativeDraw();
    }
    const {bus, props} = makeProps(this.props, 'draw');
    this._bus = bus;
    return (
      <RNTTDrawView
        {...props}
        ref="ad"
        onTTadViewEvent={this._event}
      />
    );
  }
}

const styles = StyleSheet.create({
  card: {
    position:"absolute",
    left:6,
    right:6,
    bottom:5,
    padding:10,
    borderRadius:6,
    backgroundColor:'rgba(0,0,0,.15)',
  },
  title:{
    flexDirection:'row',
    alignItems:'center',
    paddingBottom:8,
  },
  icon:{
    width:26,
    height:26,
    borderRadius:13,
  },
  titleText:{
    color:'#fff',
    fontSize:14,
    marginLeft:10,
  },
  button:{
    backgroundColor:'#d33f57',
    paddingVertical:4,
    paddingHorizontal:18,
    borderRadius:2,
    marginLeft:10,
  },
  buttonText:{
    color:'#fff',
    fontSize:11,
  },
  logo:{
    width:10,
    height:10,
  },
  description:{
    color:'#fff',
    fontSize:10,
    lineHeight:16,
  },
});
export default TTAdDraw;