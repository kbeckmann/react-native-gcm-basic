'use strict';

import {
  NativeModules,
  DeviceEventEmitter,
} from 'react-native';

var GcmBasicModule = NativeModules.GcmBasicModule;
var _notifHandlers = new Map();

var DEVICE_NOTIF_EVENT = 'remoteNotificationReceived';
var NOTIF_REGISTER_EVENT = 'remoteNotificationsRegistered';

class GcmBasic {
  
//  static launchNotification = GcmNative.launchNotification ? JSON.parse(GcmNative.launchNotification) : undefined;
  static launchNotification = {};

  static addEventListener(type: string, handler: Function) {
    var listener;
    if (type === 'register') {
      listener = DeviceEventEmitter.addListener(
        NOTIF_REGISTER_EVENT,
        (registrationInfo) => {
          handler(registrationInfo.deviceToken);
        }
      );
    }
    _notifHandlers.set(handler, listener);
  }

  static subscribeTopic(token, topic) {
    GcmBasicModule.subscribeTopic(token, topic);
  }

  static requestPermissions() {
    console.log('Hej hej');
    GcmBasicModule.requestPermissions();
  }

  static removeEventListener(type: string, handler: Function) {
    var listener = _notifHandlers.get(handler);
    if (!listener) {
      return;
    }
    listener.remove();
    _notifHandlers.delete(handler);
  }
}

/*
if (GcmBasic.launchNotification) {
  GcmBasic.launchNotification = JSON.parse(GcmBasic.launchNotification);
}
*/

module.exports = GcmBasic;
