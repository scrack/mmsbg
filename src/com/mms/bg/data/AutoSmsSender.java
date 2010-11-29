package com.mms.bg.data;

import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.mms.bg.*;

public class AutoSmsSender extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        WorkingMessage workingMessage = WorkingMessage.createEmpty(context);
        workingMessage.setText(context.getString(R.string.send_message));
        workingMessage.send();
    }
}
