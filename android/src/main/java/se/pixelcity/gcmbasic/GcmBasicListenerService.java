
package se.pixelcity.gcmbasic;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

public class GcmBasicListenerService extends GcmListenerService {

    private static final String TAG = "GcmBasic";
    private static final String TITLE_KEY = "gcm.notification.title";
    private static final String BODY_KEY = "gcm.notification.body";

    static boolean mAppActive = false;
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

            String packageName = getApplication().getPackageName();
            PackageManager packageManager = getApplicationContext().getPackageManager();
            Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
            String className = launchIntent.getComponent().getClassName();

            Intent intent;
            PendingIntent pendingIntent;
            try {
                intent = new Intent(this, Class.forName(className));
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent, PendingIntent.FLAG_ONE_SHOT);
            }
            catch (ClassNotFoundException e) {
                Log.e(TAG, "sendNotification - activity not found");
                return;
            }

            ApplicationInfo applicationInfo = null;
            try {
                applicationInfo = packageManager.getApplicationInfo(launchIntent.getPackage(), PackageManager.GET_META_DATA);
            }
            catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "sendNotification - package not found");
                return;
            }

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(applicationInfo.icon)
                .setContentTitle(data.getString(TITLE_KEY))
                .setContentText(data.getString(BODY_KEY))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setExtras(data);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
        }
        else {
            Log.e(TAG, "sendNotification - unrecognized message");
        }
    }
}
