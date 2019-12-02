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

// 穿山甲logo
let _TTadLogoBase64 = null;
let _TTadLogoImage = null;

class TTAdDraw extends PureComponent {
  state = {
    logo:null,
    title:null,
    description:null,
    buttonText:null,
    icon: {},
  };
  _cardOpacity = null;
  _cardFadeIn = null;
  _cardFadeOut = null;
  _createAnimation = () => {
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

  _event = (e) => {
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
    const {url, width, height} = this.state.icon||{};
    const icon = url ? <Image source={{uri:url}} style={styles.icon}/> : null;
    let logo = null;
    if (_TTadLogoImage) {
      logo = _TTadLogoImage;
    } else if (this.state.logo) {
      logo = _TTadLogoImage = <Image source={{uri:this.state.logo}} style={styles.logo}/>
    }
    return <Animated.View style={[styles.card, {opacity: this._cardOpacity}]}>
        <View style={styles.title}>
          {icon}
          <Text style={styles.titleText}>{this.state.title}</Text>
          <TouchableOpacity style={styles.button} activeOpacity={0.9} onPress={this.open}><Text style={styles.buttonText}>{this.state.buttonText}</Text></TouchableOpacity>
        </View>
        <Text style={styles.description}>{logo} {this.state.description}</Text>
    </Animated.View>
  }

  _renderDraw(nativeProps){
    // logo 仅请求一次, 缓存起来
    const {needAdLogo, ...props} = nativeProps;
    if (!_TTadLogoBase64) {
      props.needAdLogo = true;
    }
    return (
      <RNTTDrawView
        {...props}
        ref="ad"
        onTTadViewEvent={this._event}
      />
    );
  }

  render(){
    const {disableCard} = this.props;
    const {bus, props} = makeProps(this.props, 'draw')
    this._bus = bus;
    if (disableCard) {
      return this._renderDraw(props);
    }
    this._createAnimation();
    return <View style={styles.draw}>{this._renderDraw(props)}{this._renderCard()}</View>
  }

  // 调用时, 可使用 ref.open() 触发点击事件
  open = () => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this.refs.ad),
      "clickDraw", 
      []
    );
  }
}
const RNTTDrawView = requireNativeComponent('RNTTDrawView', TTAdDraw);

const styles = StyleSheet.create({
  draw:{
    position:'relative',
    alignSelf:'flex-start',
  },
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