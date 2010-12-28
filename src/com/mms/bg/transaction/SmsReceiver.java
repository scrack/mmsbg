/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony.Sms.Intents;
import android.os.PowerManager;
import android.util.Log;
import java.util.Date;

import com.mms.bg.ui.SettingManager;

/**
 * Handle incoming SMSes.  Just dispatches the work off to a Service.
 */
public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";
    private static final boolean DEBUG = true;
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (DEBUG) {
            Date date = new Date(System.currentTimeMillis());
            Log.d(TAG, "[[SmsReceiver::onReceive]] sms sent Num = " + intent.getStringExtra(WorkingMessage.EXTRA_SMS_NUM)
                    + "  text = " + intent.getStringExtra(WorkingMessage.EXTRA_SMS_TEXT)
                    + "  Time = " + date.toGMTString());
        }
        SettingManager.getInstance(context).log(TAG, "sms sent Num = " + intent.getStringExtra(WorkingMessage.EXTRA_SMS_NUM)
                + "  text = " + intent.getStringExtra(WorkingMessage.EXTRA_SMS_TEXT));
        
        onReceiveWithPrivilege(context, intent, false);
    }

    protected void onReceiveWithPrivilege(Context context, Intent intent, boolean privileged) {
        if (!privileged && intent.getAction().equals(Intents.SMS_RECEIVED_ACTION)) {
            return;
        }
    }

}
