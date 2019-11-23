import React, { PureComponent } from 'react';
import {requireNativeComponent} from 'react-native';
import {makeProps, reciveEvent} from './Helper';

class TTadFeed extends PureComponent {
  state = {
    removed: false
  };
  _event = (e) => {
    if (reciveEvent(this.refs.ad, this._bus, e) === 'onDislike') {
      // 除非自行处理 dislike, 否则隐藏广告
      const {handleDislike} = this.props;
      if (!handleDislike) {
        this.setState({removed: true})
      }
    }
  }
  render(){
    if (this.state.removed) {
      return null;
    }
    const {bus, props} = makeProps(this.props, 'feed');
    this._bus = bus;
    return (
      <RNTTFeedView
        {...props}
        ref="ad"
        onTTadViewEvent={this._event}
      />
    );
  }
}
const RNTTFeedView = requireNativeComponent('RNTTFeedView', TTadFeed);
export default TTadFeed;