package com.mms.bg.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class InternetStatusReceiver extends BroadcastReceiver {
    private static final String TAG = "InternetStatusReceiver";
    private static final boolean DEBUG = true;
    
    @Override
    public void onReceive(Context context, Intent intent) {
        LOGD("receive the internet status changed");
        SettingManager sm = SettingManager.getInstance(context);
        sm.log("receive the internet status changed");
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                                                        .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        LOGD("info = " + info + " info.isAvailable() = " + (info != null ? info.isAvailable() : false));
        if (info != null && info.isAvailable() == true
                && (sm.getInternetConnectFailed() == true
                        || sm.getInternetConnectFailedBeforeSMS() == true)) {
            LOGD("start servie for internet available");
            SettingManager.getInstance(context).log("find available internet");
            Intent intent1 = new Intent(context, BgService.class);
            intent1.setAction(BgService.ACTION_INTERNET_CHANGED);
            context.startService(intent1);
        }
    }
    
    public final void LOGD(String msg) {
        if (DEBUG) {
            Log.d(TAG, "[[" + this.getClass().getSimpleName()
                    + "::" + Thread.currentThread().getStackTrace()[3].getMethodName()
                    + "]] " + msg);
        }
    }
}
