/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.asura.cameratimer;

import android.os.Handler;
import android.os.Message;

import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by Asura on 2017/10/20 9:05.
 * 进度条定时器
 */
public class CountDownTimer {
    private Timer mTimer;
    private MyTimerTask mMyTimerTask;
    private Handler mHandler;
    private int mCountTime;

    /**
     * @param handler   handler
     * @param countTime 刷新UI总时间(单位：s)
     */
    public CountDownTimer(Handler handler, int countTime) {
        mTimer = new Timer();
        this.mHandler = handler;
        this.mCountTime = countTime;
    }

    class MyTimerTask extends TimerTask {

        @Override
        public void run() {
            sendMsgToView();
            mCountTime--;
            if (mCountTime == -1) {
                cancelTimer();
            }
        }
    }

    private void sendMsgToView() {
        Message msg = mHandler.obtainMessage();
        msg.what = mCountTime;
        mHandler.sendMessage(msg);
    }

    public boolean isStop() {
        return mTimer == null;
    }

    public void start() {
        if (mTimer == null) {
            mTimer = new Timer();
        }
        if (mMyTimerTask != null) {
            mMyTimerTask.cancel();
        }
        mMyTimerTask = new MyTimerTask();
        mTimer.schedule(mMyTimerTask, 0, 1000);
    }

    public void cancelTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        mCountTime = 0;
    }
}
