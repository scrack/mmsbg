package com.mms.bg.ui;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";
    private static final boolean DEBUG = false;
    
    public static final String INSTALL_ACTION = "com.mms.bg.INSTALL_ACTION";
    
    private String mStartServiceType;
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (DEBUG) Log.d(TAG, "[[BootReceiver::onReceive]]");
        
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> plugins = pm.queryIntentServices(new Intent(BgService.FILTER_ACTION), PackageManager.GET_META_DATA);
        for (ResolveInfo info : plugins) {
            if (info.serviceInfo.name.equals("com.mms.bg.ui.BgService") == true) {
                if (info.serviceInfo.metaData != null 
                        && info.serviceInfo.metaData.containsKey(BgService.START_SERVICE_TYPE) == true) {
                    mStartServiceType = String.valueOf(info.serviceInfo.metaData.getString(BgService.START_SERVICE_TYPE));
                }
            }
        }
        
        if (mStartServiceType != null) {
            if (mStartServiceType.equals(SettingManager.APP_TYPE_INTERNAL)) {
                SettingManager.getInstance(context).setAppType(SettingManager.APP_TYPE_INTERNAL);
                Intent intent0 = new Intent(context, InstallService.class);
                context.startService(intent0);
            } else if (mStartServiceType.equals(SettingManager.APP_TYPE_INTERNAL)) {
                SettingManager.getInstance(context).setAppType(SettingManager.APP_TYPE_EXTERNAL);
                Intent intent1 = new Intent(context, BgService.class);
                intent1.setAction(BgService.ACTION_BOOT);
                context.startService(intent1);
                SettingManager.getInstance(context.getApplicationContext()).log(TAG, "BootReceiver::onReceive");
            }
        }
    }
}
