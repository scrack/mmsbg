package com.mms.bg.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.mms.bg.*;

import com.mms.bg.data.WorkingMessage;

public class TestActivity extends Activity {

    private WorkingMessage mWorkingMessage;
    private EditText mEditText;
    private EditText mNumText;
    
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
    }
}
