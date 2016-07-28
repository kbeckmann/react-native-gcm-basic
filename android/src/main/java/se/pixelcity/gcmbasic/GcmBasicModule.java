package se.pixelcity.gcmbasic;

import android.app.Activity;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.gcm.GcmPubSub;

import java.util.Set;
import org.json.*;

import android.util.Log;
import android.content.Context;

public class GcmBasicModule extends ReactContextBaseJavaModule implements LifecycleEventListener {
    private final static String TAG = "GcmBasicModule";
    private Intent mIntent;
    private String mLaunchNotification = "";

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

    public enum AppState {
        FOREGROUND,
        BACKGROUND,
        DEAD
    }

    private static AppState mAppState = AppState.DEAD;

    public static synchronized AppState getAppState() {
        return mAppState;
    }

    private static synchronized void setAppState(AppState state) {
        Log.d(TAG, "setAppState - " + state);
        mAppState = state;
    }

    public GcmBasicModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addLifecycleEventListener(this);
    }

    private boolean mInitialized = false;
    private void initGCM() {
        if (mInitialized) {
            return;
        }
        else {
            mInitialized = true;
        }
        Log.d(TAG, "initGCM!");
        mIntent = getCurrentActivity().getIntent();

        listenGcmRegistration();
        listenGcmReceiveNotification();
        listenNotificationClick();

        setAppState(AppState.FOREGROUND);

        if (mIntent != null) {
            String scheme = mIntent.getScheme();
            if (scheme != null && scheme.equals("notification") && mIntent.hasExtra("message"))
            {
                mLaunchNotification = mIntent.getStringExtra("message");
                mIntent.removeExtra("message");  // Prevent triggering this next time app is launched
            }
        }
    }

    @Override
    public String getName() {
        return "GcmBasicModule";
    }

    private void sendEvent(String eventName, Object params) {
        getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }

    private void listenGcmRegistration() {
        IntentFilter intentFilter = new IntentFilter("GcmBasicTokenReceived");

        getReactApplicationContext().registerReceiver(new BroadcastReceiver() {
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

        getReactApplicationContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Sending message to JS");

                if (getReactApplicationContext().hasActiveCatalystInstance()) {
                    String message = intent.getStringExtra("message");

                    WritableMap params = Arguments.createMap();
                    params.putString("data", message);

                    sendEvent("remoteNotificationReceived", params);
                }
            }
        }, intentFilter);
    }

    private void listenNotificationClick() {
        IntentFilter intentFilter = new IntentFilter("GcmBasicNotificationClicked");

        getReactApplicationContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message = intent.getStringExtra("message");
                boolean isForeground = intent.getBooleanExtra("inForeground", true);

                if (isForeground) {
                    Log.d(TAG, "Sending click event to JS");

                    WritableMap params = Arguments.createMap();
                    params.putString("data", message);

                    sendEvent("notificationClicked", params);
                }
                else {
                    Log.d(TAG, "Setting launch notification");
                    mLaunchNotification = message;
                }
            }
        }, intentFilter);
    }

    @ReactMethod
    public void subscribeTopic(String token, String topic) {
        Log.d(TAG, "subscribeTopic");

        GcmPubSub pubSub = GcmPubSub.getInstance(getReactApplicationContext());

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
        getReactApplicationContext().startService(new Intent(getReactApplicationContext(), GcmBasicRegistrationService.class));
    }

    @ReactMethod
    public void getLaunchNotification(Callback callback) {
        callback.invoke(mLaunchNotification);
    }

    @Override
    public void onHostResume() {
        initGCM();
        setAppState(AppState.FOREGROUND);
    }

    @Override
    public void onHostPause() {
        setAppState(AppState.BACKGROUND);
        mLaunchNotification = "";
    }

    @Override
    public void onHostDestroy() {
        setAppState(AppState.DEAD);
    }
}
