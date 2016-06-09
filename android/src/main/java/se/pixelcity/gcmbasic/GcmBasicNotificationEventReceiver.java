package se.pixelcity.gcmbasic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class GcmBasicNotificationEventReceiver extends BroadcastReceiver {
    private final static String TAG = "GcmBasicNotifEeventRec";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onRecieve - got intent");

        Bundle data = intent.getExtras();
        String dataString = GcmBasicModule.convertJSON(data);

        GcmBasicModule.AppState state = GcmBasicModule.getAppState();
        switch (state) {
            case FOREGROUND:
                sendNotificationIntent(context, dataString, true);
                break;
            case BACKGROUND:
                sendNotificationIntent(context, dataString, false);
                break;
            case DEAD:
                sendLaunchIntent(context, dataString);
                break;
        }
    }

    private void sendLaunchIntent(Context context, String data) {
        String packageName = context.getApplicationContext().getPackageName();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        launchIntent.putExtra("message", data);
        launchIntent.setData(Uri.parse("notification://clicked"));

        context.startActivity(launchIntent);
        Log.d(TAG, "sendLaunchIntent - launching: " + packageName);
    }

    private void sendNotificationIntent(Context context, String data, boolean inForeground) {
        Intent broadcastIntent = new Intent("GcmBasicNotificationClicked");

        broadcastIntent.putExtra("message", data);
        broadcastIntent.putExtra("inForeground", inForeground);

        context.sendBroadcast(broadcastIntent);
        Log.d(TAG, "sendNotificationIntent - send broadcast, fg: " + inForeground);
    }
}
