package se.pixelcity.gcmbasic;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

public class GcmBasicRegistrationService extends IntentService {

    private final static String TAG = "GcmBasicRegistrationService";
    private static final String[] TOPICS = {"global"};

    public GcmBasicRegistrationService() {
        super(TAG);

        Log.d(TAG, "Registration server started");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Resources resources = getApplication().getResources();
        String packageName = getApplication().getPackageName();
        int resourceId = resources.getIdentifier("gcm_defaultSenderId", "string", packageName);
        String senderId = getString(resourceId);

        try {
            // Initially this call goes out to the network to retrieve the token, subsequent calls
            // are local.
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(senderId, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

            Log.d(TAG, "GCM Registration Token: " + token);

            // Notify module of the new token so it can send an event to JS
            Intent tokenRecieved = new Intent("GcmBasicTokenReceived");
            tokenRecieved.putExtra("token", token);
            sendBroadcast(tokenRecieved);
        }
        catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
        }
    }

}