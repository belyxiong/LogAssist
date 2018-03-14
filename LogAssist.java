package com.bely.utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.Log;

/*
 * This class is used to cache log inside memory and print it out at the time when we want to check it.
 *
 * Enable the function:
 *
 * By default it is disabled, to enabled it, create an empty file named as "logassist_enabled" under
 * the specific path defined by "SWITCHER_FILE".
 *
 * Print:
 * To trigger the print, just send a broadcast from adb console using command :
 * 
 * am broadcast -a action.com.bely.logassist.print
 *
 * by default it will print the cached log immediately, if you have another adb console with logcat running
 * you will see it using :
 * 
 * logcat -s AudioAppLogAssist
 * 
 * But if you only have one adb console, you may miss it after you send the broadcast, then you can choose to
 * delay the print, to delay it, sending broadcast with a delayed integer value , like following:
 * 
 * am broadcast -a action.com.bely.logassist.print --ei delay 5000
 * 
 * 5000 above means delay 5 seconds to print, then you will chance to run logcat before it print.
 *
 * Control:
 *
 * Also you can pause/resume the log, or clear the cache buffer using following intent:
 *
 * action.com.bely.logassist.control.pause
 * action.com.bely.logassist.control.resume
 * action.com.bely.logassist.control.clear
 *
 */
public class LogAssist {
    private static final String TAG = "AudioAppLogAssist";

    //control command
    private static final String ACTION_PRINT = "action.com.bely.logassist.print";
    private static final String ACTION_CONTROL_PAUSE = "action.com.bely.logassist.control.pause";
    private static final String ACTION_CONTROL_RESUME = "action.com.bely.logassist.control.resume";
    private static final String ACTION_CONTROL_CLEAR = "action.com.bely.logassist.control.clear";

    //delay time to print the buffered log (ms)
    private static final String ACTION_EXTRA_DELAY2PRINT = "delay";
    //msg to print the log
    private static final int MSG_PRINT = 2008;
    //switch the log assist on or off by adding/removing this file
    private static final String SWITCHER_FILE = "/data/data/com.bely/logassist_enabled";
    //maxium buffer used to cache the log
    private static final int MAX_BUFFER_SIZE = 10241000;
    //buffer size to clear when it is full
    private static final int BUFFER_SIZE_TO_CLEAR = 512;
    //start line of the log
    private static final String STR_LOG_START_LINE = "---------------- LogAssist Print------------\n";
    static StringBuffer mLogBuffer;
    static SimpleDateFormat mDateFormat;
    static boolean mStarted = false;
    static int mPid;
    static Handler mPrintHandler = null;

    public static void init(Context context) {
        if (!needStart()) {
            Log.d(TAG, "LogAssist is not enabled.");
            return;
        }
        mLogBuffer = new StringBuffer(STR_LOG_START_LINE);
        mDateFormat = new SimpleDateFormat("dd-M-yyyy hh:mm:ss.SSS");
        mStarted = true;
        mPid = Process.myPid();
        registerControlReceiver(context);
        Log.d(TAG, "LogAssist is enabled.");
    }

    static class PrintHandlerCallBack implements Handler.Callback {
        
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_PRINT:
                print();
                break;
            }
            return false;
        }
    }
    private static void registerControlReceiver(Context context) {
        BroadcastReceiver controlReceiver = new BroadcastReceiver() {

            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "received intent :"+intent.getAction());
                if (intent == null) {
                    return;
                }
                String action = intent.getAction();
                if (ACTION_PRINT.equals(action)) {
                    int delay = intent.getIntExtra(ACTION_EXTRA_DELAY2PRINT, 0);
                    Log.d(TAG, " intent :delay="+delay);
                    if (delay <=0) {
                        print();
                    } else {
                        if (mPrintHandler == null) {
                            mPrintHandler = new Handler(new PrintHandlerCallBack());
                        }
                        mPrintHandler.sendEmptyMessageDelayed(MSG_PRINT, delay);
                    }
                } else if (ACTION_CONTROL_PAUSE.equals(action)) {
                    mStarted = false;
                }else if (ACTION_CONTROL_RESUME.equals(action)) {
                    mStarted = true;
                }else if (ACTION_CONTROL_CLEAR.equals(action)) {
                    mLogBuffer = new StringBuffer(STR_LOG_START_LINE);
                }
            }
        };
        IntentFilter ctrlIntentFilter = new IntentFilter();
        ctrlIntentFilter.addAction(ACTION_PRINT);
        ctrlIntentFilter.addAction(ACTION_CONTROL_PAUSE);
        ctrlIntentFilter.addAction(ACTION_CONTROL_RESUME);
        ctrlIntentFilter.addAction(ACTION_CONTROL_CLEAR);

        context.registerReceiver(controlReceiver, ctrlIntentFilter);
    }

    //check the switch file to see we need run log assist
    private static boolean needStart() {
        File switcher = new File(SWITCHER_FILE);
        return switcher.exists();
    }

    public static void log(String info) {
        if (!mStarted) {
            return;
        }
        if (mLogBuffer.length() >= MAX_BUFFER_SIZE) {
            //if buffer is full, remove first part defined by BUFFER_SIZE_TO_CLEAR
            if (mLogBuffer.length() > BUFFER_SIZE_TO_CLEAR) {
                //if the cached size is bigger than size to be cleared
                mLogBuffer.delete(0, BUFFER_SIZE_TO_CLEAR);
            } else {
                //if the cached size is not bigger than size to be cleared, reset the buffer
                mLogBuffer = new StringBuffer(STR_LOG_START_LINE);
            }
        }
        String date = mDateFormat.format(new Date());
        int tid = Process.myTid();

        mLogBuffer.append(date + "  " + mPid + "  " + tid + "    " + info + "\n");
    }

    private static void print() {
        if (mLogBuffer != null) {
            String[] lines = mLogBuffer.toString().split("\n");
            for( String line : lines) {
                Log.i(TAG, line );
            }
        }
    }
}
