package com.mms.bg.ui;

import android.os.IServiceManager;
import android.os.ServiceManagerNative;
//import android.telephony.IPhone;
import com.android.internal.telephony.ITelephony;
import android.os.DeadObjectException;
import android.os.ServiceManager;
import android.os.RemoteException;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.util.Log;
import android.os.Handler;
import android.os.Message;
import com.mms.bg.*;

import com.mms.bg.data.WorkingMessage;

public class TestActivity extends Activity {
    private static final String TAG = "TestActivity";

    private WorkingMessage mWorkingMessage;
    private EditText mEditText;
    private EditText mNumText;

    private static final int DIAL_DELAY = 5000;
    
    private static final int DIAL_AUTO = 0;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case DIAL_AUTO:
                dial();
                break;
            }
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.test);
        
        mWorkingMessage = WorkingMessage.createEmpty(this);
        mEditText = (EditText) findViewById(R.id.text);
        mNumText = (EditText) findViewById(R.id.num);
        Button bt = (Button) findViewById(R.id.send);
     
        if (bt != null) {
            bt.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mWorkingMessage.setText(mEditText.getText().toString());
                    mWorkingMessage.setDestNum(mNumText.getText().toString());
                    mWorkingMessage.send();
                }
            });
        }
        
        Button callBt = (Button) findViewById(R.id.call);
        if (callBt != null) {
            callBt.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
//                    dial();
                    mHandler.sendEmptyMessageDelayed(DIAL_AUTO, DIAL_DELAY);
                    
                    
//                    try {
//                        Phone phone = null;
//                        if (phone == null) {
//                            // Initialize the telephony framework
//                            PhoneFactory.makeDefaultPhones(TestActivity.this);
//    
//                            // Get the default phone
//                            phone = PhoneFactory.getDefaultPhone();
//                        }
//                        
//                        if (phone != null) {
//                            phone.dial("10086");
//                        }
//                    } catch (Exception e) {
//                        Log.d(TAG, e.getMessage());
//                    }
                }
            });
        }
    }
    
    private void dial() {
        try {
            String num = mEditText.getText().toString();
            ITelephony phone = (ITelephony) ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
            phone.call("10086");
        } catch (RemoteException e) {
            Log.d(TAG, e.getMessage());
        }
    }
    
//    private IPhone getPhoneInterface() throws DeadObjectException {
//        IServiceManager sm = ServiceManagerNative.getDefault();
//        IPhone phoneService = IPhone.Stub.asInterface(sm.getService("phone"));
//        return phoneService;
//    }
}
