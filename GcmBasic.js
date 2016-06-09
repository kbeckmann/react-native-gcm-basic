'use strict';

import {
  NativeModules,
  DeviceEventEmitter,
} from 'react-native';

var GcmBasicModule = NativeModules.GcmBasicModule;
var _notifHandlers = new Map();

var DEVICE_NOTIF_EVENT = 'remoteNotificationReceived';
var NOTIF_REGISTER_EVENT = 'remoteNotificationsRegistered';
var NOTIF_CLICK_EVENT = 'notificationClicked';

class GcmBasic {
  
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
    else if (type === 'notification') {
      listener = DeviceEventEmitter.addListener(
        DEVICE_NOTIF_EVENT,
        (message) => {
          handler(JSON.parse(message.data));
        }
      );
    }
    else if (type == "notificationclick") {
      listener = DeviceEventEmitter.addListener(
        NOTIF_CLICK_EVENT,
        (message) => {
          handler(JSON.parse(message.data));
        }
      );
    }

    _notifHandlers.set(handler, listener);
  }

  static getLaunchNotification() {
    var promise = new Promise(function(resolve, reject) {
      GcmBasicModule.getLaunchNotification((notifString) => {
        if (notifString != '') {
          resolve(JSON.parse(notifString));
        }
        else {
          resolve(undefined);
        }
      });
    });

    return promise;
  }

  static subscribeTopic(token, topic) {
    GcmBasicModule.subscribeTopic(token, topic);
  }

  static requestPermissions() {
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

module.exports = GcmBasic;
