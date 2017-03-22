
package se.pixelcity.gcmbasic;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.graphics.drawable.Drawable;

import com.google.android.gms.gcm.GcmListenerService;

public class GcmBasicListenerService extends GcmListenerService {

    private final static String TAG = "GcmBasicListenerService";
    private static final String TITLE_KEY = "gcm.notification.title";
    private static final String BODY_KEY = "gcm.notification.body";

    private Bitmap appIcon = null;

    @Override
    public void onMessageReceived(String from, Bundle data) {
        boolean isActive = GcmBasicModule.getAppState() == GcmBasicModule.AppState.FOREGROUND;
        Log.d(TAG, "onMessageReceived, active: " + isActive);

        if (isActive) {
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

    // https://stackoverflow.com/questions/3035692/how-to-convert-a-drawable-to-a-bitmap
    private static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private void sendNotification(Bundle data) {
        if (data.containsKey(TITLE_KEY) && data.containsKey(BODY_KEY)) {
            Log.d(TAG, "sendNotification - displaying notification");

            String packageName = getApplication().getPackageName();
            PackageManager packageManager = getApplicationContext().getPackageManager();
            Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
            String className = launchIntent.getComponent().getClassName();

            Intent intent = new Intent(getApplicationContext(), GcmBasicNotificationEventReceiver.class);
            intent.putExtras(data);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            ApplicationInfo applicationInfo = null;
            try {
                applicationInfo = packageManager.getApplicationInfo(launchIntent.getPackage(), PackageManager.GET_META_DATA);
            }
            catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "sendNotification - package not found");
                return;
            }

            if (this.appIcon == null) {
                this.appIcon = drawableToBitmap(packageManager.getApplicationIcon(applicationInfo));
            }

            // TODO: Make icons configurable
            int appIconResourceID = android.R.drawable.ic_dialog_email;

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setLargeIcon(this.appIcon)
                .setSmallIcon(appIconResourceID)
                .setContentTitle(data.getString(TITLE_KEY))
                .setContentText(data.getString(BODY_KEY))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
        }
        else {
            Log.e(TAG, "sendNotification - unrecognized message");
        }
    }
}
