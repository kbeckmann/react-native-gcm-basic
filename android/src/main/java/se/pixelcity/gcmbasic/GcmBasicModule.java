package se.pixelcity.gcmbasic;

import android.app.Activity;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.gcm.GcmPubSub;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.json.*;

import android.preference.PreferenceManager;
import android.util.Log;

import android.content.Context;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.res.Resources;
import android.app.PendingIntent;
import android.support.v4.app.NotificationCompat;
import android.app.Notification;
import android.app.NotificationManager;
import android.media.RingtoneManager;
import android.net.Uri;

public class GcmBasicModule extends ReactContextBaseJavaModule implements LifecycleEventListener {
    private final static String TAG = GcmBasicModule.class.getCanonicalName();
    private ReactContext mReactContext;
    private Intent mIntent;
    private Activity mActivity;

    public static String convertJSON(Bundle bundle) {
        JSONObject json = new JSONObject();
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    json.put(key, JSONObject.wrap(bundle.get(key)));
                } else {
                    json.put(key, bundle.get(key));
                }
            } catch(JSONException e) {
                return null;
            }
        }
        return json.toString();
    }

    public GcmBasicModule(ReactApplicationContext reactContext, Intent intent, Activity activity) {
        super(reactContext);
        mReactContext = reactContext;
        mActivity = activity;

        // todo later
        //        mIntent = intent;
        mIntent = null;

        if (activity != null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mReactContext);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("GcmMainActivity", activity.getClass().getSimpleName());
            editor.apply();
        }

        if (mIntent == null) {
            listenGcmRegistration();
            listenGcmReceiveNotification();
            getReactApplicationContext().addLifecycleEventListener(this);
        }
    }

    @Override
    public String getName() {
        return "GcmBasicModule";
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        if (mIntent != null) {
            Bundle bundle = mIntent.getBundleExtra("bundle");
            String bundleString = convertJSON(bundle);
            constants.put("launchNotification", bundleString);
        }
        return constants;
    }

    private void sendEvent(String eventName, Object params) {
        mReactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }

    private void listenGcmRegistration() {
        IntentFilter intentFilter = new IntentFilter("GcmBasicTokenReceived");

        mReactContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Sending token to JS");

                String token = intent.getStringExtra("token");
                WritableMap params = Arguments.createMap();
                params.putString("deviceToken", token);

                sendEvent("remoteNotificationsRegistered", params);
            }
        }, intentFilter);
    }

    private void listenGcmReceiveNotification() {
        IntentFilter intentFilter = new IntentFilter("GcmBasicMessageReceived");

        mReactContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Sending message to JS");

                if (mReactContext.hasActiveCatalystInstance()) {
                    String message = intent.getStringExtra("message");

                    WritableMap params = Arguments.createMap();
                    params.putString("data", message);

                    sendEvent("remoteNotificationReceived", params);
                }
            }
        }, intentFilter);
    }

    @ReactMethod
    public void subscribeTopic(String token, String topic) {
        Log.d(TAG, "subscribeTopic");

        GcmPubSub pubSub = GcmPubSub.getInstance(this.mReactContext);

        try {
            pubSub.subscribe(token, topic, null);
        }
        catch (java.io.IOException e) {
            Log.e(TAG, "Failed to register topic");
        }
    }

    @ReactMethod
    public void requestPermissions() {
        Log.d(TAG, "requestPermissions");
        mReactContext.startService(new Intent(mReactContext, GcmBasicListenerService.class));
        mReactContext.startService(new Intent(mReactContext, GcmBasicRegistrationService.class));

        GcmBasicListenerService.setAppActive(true);
    }

    /*
    @ReactMethod
    public void stopService() {
        if (mIntent != null) {
            new android.os.Handler().postDelayed(new Runnable() {
                public void run() {
                    mReactContext.stopService(mIntent);
                }
            }, 1000);
        }
    }
    */

    /*
    private Class getMainActivityClass() {
        try {
            String packageName = mReactContext.getPackageName();

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mReactContext);
            String activityString = preferences.getString("GcmMainActivity", null);
            if (activityString == null) {
                Log.d(TAG, "GcmMainActivity is null");
                return null;
            } else {
                return Class.forName(packageName + "." + activityString);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
    */

    @Override
    public void onHostResume() {
        GcmBasicListenerService.setAppActive(true);
    }

    @Override
    public void onHostPause() {
        GcmBasicListenerService.setAppActive(false);
    }

    @Override
    public void onHostDestroy() {
        GcmBasicListenerService.setAppActive(false);
    }
}
