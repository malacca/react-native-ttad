import React from 'react';
import {StyleSheet, Button, View, Text} from 'react-native';
import TTad, {
  TTadBanner, 
  TTadFeed, 
  TTAdInteraction, 
  TTAdSplash, 
  TTAdDraw
} from 'react-native-ttad';

const config = {
  appId: "",

  splashId: "",
  bannerId: "",
  feedId: "",

  // 信息流 draw
  drawId:"",
  drawNativeId:"",

  // 全屏
  fullVideoId:"",
  fullVideoNativeId:"",

  //激励
  rewardVideoId:"",
  rewardVideoNativeId:"",

  // 插屏
  interactionId:"",
}

TTad.initSdk({
  appId:config.appId,
  lightBar:false,
  debug:true
});

class TTadTest extends React.Component {
  state = {
    adType: null,
    uuid: null,
  };

  listener = (bus) => {
    bus.onFail(e => {
      console.log('__onFail', e)
    })
    .onLoad(e => {
      console.log('__onLoad', e)
    })
    .onShow(e => {
      console.log('__onShow', e)
    })
    .onSkip(e => {
      console.log('__onSkip', e)
    })
    .onTimeOver(e => {
      console.log('__onTimeOver', e)
    })
    .onClick(e => {
      console.log('__onClick', e)
    })
    .onOpen(e => {
      console.log('__onOpen', e)
    })
    .onClose(e => {
      console.log('__onClose', e)
    })
    .onDislike(e => {
      console.log('__onDislike', e)
    })
    .onVideoLoad(e => {
      console.log('__onVideoLoad', e)
    })
    .onVideoComplete(e => {
      console.log('__onVideoComplete', e)
    })
  }

  
  setAd = (adType) => {
    this.setState({
      adType
    })
  }


  preload = (adType) => {
    let repload, codeId, native = false;
    if (adType === 'feed') {
      repload = TTad.loadFeed;
      codeId = config.feedId;
    } else if (adType === 'draw') {
      repload = TTad.loadDraw;
      codeId = config.drawId;
    } else if (adType === 'drawNative') {
      repload = TTad.loadDraw;
      codeId = config.drawNativeId;
      native = true;
    }
    console.log(codeId, native);
    repload(codeId, 1)
    .isNative(native)
    .onError(e => {
      console.log('_____preload_error', e)
    })
    .onLoad(uuids => {
      console.log('_____preload_success', uuids)
      if (uuids.length) {
        this.setState({
          adType: adType + 'Pre',
          uuid: uuids[0]
        })
      }
    }).load()
  }


  // 广告组件
  renderAd = () => {
    console.log('__render__ad', this.state.adType)
    if (this.state.adType === 'splash') {
      return <TTAdSplash
        style={styles.splash}
        codeId={config.splashId}
        listener={this.listener}
      />
    }

    if (this.state.adType === 'banner') {
      return <TTadBanner
        style={styles.banner}
        codeId={config.bannerId}
        listener={this.listener}
      />
    }

    if (this.state.adType === 'interaction') {
      return <TTAdInteraction
        style={styles.interaction}
        codeId={config.interactionId}
        listener={this.listener}
      />
    }

    if (this.state.adType === 'feed') {
      return <TTadFeed
        style={styles.feed}
        codeId={config.feedId}
        listener={this.listener}
      />
    }
    if (this.state.adType === 'feedPre') {
      return <TTadFeed
        style={styles.feed}
        uuid={this.state.uuid}
        listener={this.listener}
      />
    }



    if (this.state.adType === 'draw') {
      return <TTAdDraw
        style={styles.draw}
        codeId={config.drawId}
        canInterrupt={true}
        listener={this.listener}
      />
    }
    if (this.state.adType === 'drawPre') {
      return <TTAdDraw
        style={styles.draw}
        uuid={this.state.uuid}
        canInterrupt={true}
        listener={this.listener}
      />
    }


    if (this.state.adType === 'drawNative') {
      return <TTAdDraw
        style={styles.draw}
        codeId={config.drawNativeId}
        listener={this.listener}
        canInterrupt={true}
        native={true}
      />
    }
    if (this.state.adType === 'drawNativePre') {
      return <TTAdDraw
        style={styles.draw}
        uuid={this.state.uuid}
        listener={this.listener}
        canInterrupt={true}
        native={true}
      />
    }

    return null;
  }


  // API 方式加载: 全屏 / 激励 / 插屏
  loadAd = (type, native) => {
    const load = TTad[type];
    const realType = type + (native ? 'Native' : '');
    const codeId = config[realType + 'Id'];
    console.log('____startLoad', realType, codeId)
    load(codeId)
    .isNative(native)
    .onLoad(player => {
      console.log('___onLoad', player)
      if(type === 'interaction') {
        this.showAd(player)
      }
    }).onError(e => {
      console.log('___onError', e)
    }).onCached(player => {
      console.log('___onCached', player)
      this.showAd(player)
    }).load();
  }

  showAd = (player) => {
    player.onError(e => {
      console.log('___showad_onError', e)
    }).onShow(e => {
      console.log('___showad_onShow', e)
    }).onClick(e => {
      console.log('___showad_onClick', e)
    }).onSkip(e => {
      console.log('___showad_onSkip', e)
    }).onComplete(e => {
      console.log('___showad_onComplete', e)
    }).onReward(e => {
      console.log('___showad_onReward', e)
    }).onClose(e => {
      console.log('___showad_onClose', e)
    }).show();
  }

  render() {
    if (this.state.adType !== null) {
      return <View style={{flex:1}}>
        <Button title="关闭" onPress={() => this.setAd(null)} />
        <View style={styles.full}>
          {this.renderAd()}
        </View>
      </View>
    }

    return <View style={styles.full}>
      <Text>API调用</Text>
      <Button title="全屏" onPress={() => this.loadAd('fullVideo')} />
      <Button title="激励" onPress={() => this.loadAd('rewardVideo')} />
      <Button title="插屏" onPress={() => this.loadAd('interaction')} />

      <Text>组件</Text>
      <Button title="开屏" onPress={() => this.setAd('splash')} />
      <Button title="信息流" onPress={() => this.setAd('feed')} />
      <Button title="信息流(预加载)" onPress={() => this.preload('feed')} />
      <Button title="Banner" onPress={() => this.setAd('banner')} />
      <Button title="插屏" onPress={() => this.setAd('interaction')} />
      <Button title="视频信息流" onPress={() => this.setAd('draw')} />
      <Button title="视频信息流(预加载)" onPress={() => this.preload('draw')} />

      <Text>非模板自渲染(默认已无法申请)</Text>
      <Button title="全屏" onPress={() => this.loadAd('fullVideo', true)} />
      <Button title="激励" onPress={() => this.loadAd('rewardVideo', true)} />
      <Button title="视频信息流" onPress={() => this.setAd('drawNative')} />
      <Button title="视频信息流(预加载)" onPress={() => this.preload('drawNative')} />
    </View>
  }
}

const styles = StyleSheet.create({
  full:{
    flex:1,
    backgroundColor:"#fff",
    alignItems:"center",
    justifyContent:"center",
  },
  banner:{
    alignSelf:"stretch",
  },
  splash:{
    alignSelf:"stretch",
    flex:1,
  },
  feed:{
    alignSelf:"stretch",
  },
  interaction:{
    alignSelf:"stretch",
  },
  draw:{
    flex:1,
    alignSelf:"stretch",
  }
});


export default TTadTest;