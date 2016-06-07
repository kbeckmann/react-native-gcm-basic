
package se.pixelcity.gcmbasic;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import org.json.*;

import java.util.Set;

public class GcmBasicListenerService extends GcmListenerService {

    private static final String TAG = "GcmBasic";
    private static final String TITLE_KEY = "gcm.notification.title";
    private static final String BODY_KEY = "gcm.notification.body";

    static boolean mAppActive;
    public static void setAppActive(boolean active) {
        mAppActive = active;
        Log.d(TAG, "Setting appActive: " + active);
    }

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d(TAG, "onMessageReceived, active: " + mAppActive);

        if (mAppActive) {
            notifyApp(data);
        }
        else {
            sendNotification(data);
        }
    }

    private void notifyApp(Bundle data) {
        Intent messageRecieved = new Intent("GcmBasicMessageReceived");
        messageRecieved.putExtra("message", GcmBasicModule.convertJSON(data));
        sendBroadcast(messageRecieved);
    }

    /**
     * Create and show a simple notification containing the received GCM message.
     */
    private void sendNotification(Bundle data) {

        if (data.containsKey(TITLE_KEY) && data.containsKey(BODY_KEY)) {
            Log.d(TAG, "sendNotification - displaying notification");

//            Intent intent = new Intent(this, MainActivity.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
//                PendingIntent.FLAG_ONE_SHOT);
//
//        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
//                .setSmallIcon(R.drawable.ic_stat_ic_notification)
//                .setContentTitle("GCM Message")
//                .setContentText(message)
//                .setAutoCancel(true)
//                .setSound(defaultSoundUri)
//                .setContentIntent(pendingIntent);
//
//        NotificationManager notificationManager =
//                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//
//        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
        }
        else {
            Log.e(TAG, "sendNotification - unrecognized message");
        }
    }
}
