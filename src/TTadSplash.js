import React, { PureComponent } from 'react';
import {requireNativeComponent} from 'react-native';
import {makeProps, reciveEvent} from './Helper';

class TTAdSplash extends PureComponent {
  _event = (e) => {
    reciveEvent(this.refs.ad, this._bus, e);
  }
  render(){
    const {bus, props} = makeProps(this.props, 'splash');
    this._bus = bus;
    return (
      <RNTTSplashView
        {...props}
        ref="ad"
        onTTadViewEvent={this._event}
      />
    );
  }
}
const RNTTSplashView = requireNativeComponent('RNTTSplashView', TTAdSplash);
export default TTAdSplash;