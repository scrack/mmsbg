package com.mms.bg.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";
    private static final boolean DEBUG = true;
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (DEBUG) Log.d(TAG, "[[BootReceiver::onReceive]]");
        
        Intent intent1 = new Intent(context, BgService.class);
        intent1.setAction(BgService.ACTION_BOOT);
        context.startService(intent1);
    }
}
