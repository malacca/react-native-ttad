import React, { PureComponent } from 'react';
import {requireNativeComponent} from 'react-native';
import {makeProps, reciveEvent} from './Helper';

class TTAdInteraction extends PureComponent {
  _event = (e) => {
    reciveEvent(this.refs.ad, this._bus, e);
  }
  render(){
    const {bus, props} = makeProps(this.props, 'interaction');
    this._bus = bus;
    return (
      <RNInteractionView
        {...props}
        ref="ad"
        onTTadViewEvent={this._event}
      />
    );
  }
}
const RNInteractionView = requireNativeComponent('RNInteractionView', TTAdInteraction);
export default TTAdInteraction;