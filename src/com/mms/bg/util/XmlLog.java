package com.mms.bg.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import android.util.Log;
import android.util.Xml;

public class XmlLog {
    
    private static final String TAG = "XmlLog";
    private static final boolean DEBUG = true;
    
    private String mPath;
    private boolean mAutoFlush;
    private XmlSerializer mSerializer;
    private FileOutputStream mFileos;
    
    public XmlLog(String path, boolean autoFlush) {
        mPath = path;
        mAutoFlush = autoFlush;
        init(mPath);
    }
    
    public void appendLog(String tag, String msg) {
        try {
            mSerializer.startTag("", tag);
            mSerializer.attribute("", "time", msg);
            if (mAutoFlush == true) {
                mSerializer.flush();
            }
        } catch (Exception e) {
            Log.d(TAG, "[[appendLog]] e = " + e.getMessage());
        }
    }
    
    public void endLog() {
        try {
            mSerializer.endDocument();
            mFileos.close();
        } catch (Exception e) {
            Log.d(TAG, "[[endLog]] e = " + e.getMessage());
        }
    }
    
    private void init(String path) {
        File xmlFile = new File(path);
        try {
            if (xmlFile.exists() == false) {
                xmlFile.createNewFile();
            } else {
                xmlFile.delete();
            }
        } catch (IOException e) {
            Log.e("XmlLog", "[[init]] exception in createNewFile() method : " + e.getMessage());
        }
        
        try {
            mFileos = new FileOutputStream(xmlFile);
        } catch (FileNotFoundException e) {
            Log.e("XmlLog", "[[init]] can't create FileOutputStream");
        }
        
        try {
            mSerializer = Xml.newSerializer();
            mSerializer.setOutput(mFileos, "UTF-8");
            mSerializer.startDocument("UTF-8", true);
        } catch (Exception e) {
        }
    }
}
