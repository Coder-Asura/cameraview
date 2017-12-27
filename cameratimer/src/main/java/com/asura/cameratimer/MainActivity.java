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

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.cameraview.CameraView;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 实现间隔一定时间（时间可自定义）定时拍照，并压缩保存至相册
 */
public class MainActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private static final String FRAGMENT_DIALOG = "dialog";

    private CameraView mCameraView;
    private TextView tv_time;
    private EditText et_countDownTime;
    private EditText et_countPic;
    private ImageButton btn_start;
    private Button btn_stop;
    private int countTime = 30;
    private int countPic = 200;

    private Handler mBackgroundHandler;
    private CountDownTimer mCountDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCameraView = (CameraView) findViewById(R.id.camera);
        if (mCameraView != null) {
            mCameraView.addCallback(mCallback);
        }
        tv_time = (TextView) findViewById(R.id.tv_time);
        et_countDownTime = (EditText) findViewById(R.id.et_countDownTime);
        et_countPic = (EditText) findViewById(R.id.et_countPic);
        btn_start = (ImageButton) findViewById(R.id.btn_start);
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TextUtils.isEmpty(et_countDownTime.getText().toString())) {
                    countTime = Integer.parseInt(et_countDownTime.getText().toString());
                } else {
                    et_countDownTime.setText("" + countTime);
                }
                if (!TextUtils.isEmpty(et_countPic.getText().toString())) {
                    countPic = Integer.parseInt(et_countPic.getText().toString());
                } else {
                    et_countPic.setText("" + countPic);
                }
                et_countDownTime.setFocusable(false);
                et_countDownTime.setFocusableInTouchMode(false);
                et_countPic.setFocusable(false);
                et_countPic.setFocusableInTouchMode(false);
                mCountDownTimer = new CountDownTimer(new ReFreshHandler(MainActivity.this),
                        countTime);
                mCountDownTimer.start();
                btn_start.setVisibility(View.INVISIBLE);
                btn_stop.setVisibility(View.VISIBLE);
                Toast toast = Toast.makeText(MainActivity.this,
                        "开始拍摄！\n每隔   " + countTime + "  秒拍摄一次\n一个文件夹存放  " + countPic + "  张照片",
                        Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        });
        btn_stop = (Button) findViewById(R.id.btn_stop);
        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(et_countPic.getText().toString())) {
                    countPic = Integer.parseInt(et_countPic.getText().toString());
                }
                if (mCountDownTimer != null && !mCountDownTimer.isStop()) {
                    mCountDownTimer.cancelTimer();
                }
                tv_time.setText("");
                btn_start.setVisibility(View.VISIBLE);
                btn_stop.setVisibility(View.INVISIBLE);
                et_countDownTime.setFocusableInTouchMode(true);
                et_countDownTime.setFocusable(true);
                et_countPic.setFocusableInTouchMode(true);
                et_countPic.setFocusable(true);
                et_countDownTime.requestFocus();
                Toast toast = Toast.makeText(MainActivity.this, "拍摄结束!", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            mCameraView.start();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ConfirmationDialogFragment
                    .newInstance(R.string.camera_permission_confirmation,
                            new String[]{Manifest.permission.CAMERA},
                            REQUEST_CAMERA_PERMISSION,
                            R.string.camera_permission_not_granted)
                    .show(getSupportFragmentManager(), FRAGMENT_DIALOG);
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ConfirmationDialogFragment
                    .newInstance(R.string.camera_permission_confirmation,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_CAMERA_PERMISSION,
                            R.string.camera_permission_not_granted)
                    .show(getSupportFragmentManager(), FRAGMENT_DIALOG);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    protected void onPause() {
        mCameraView.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBackgroundHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundHandler.getLooper().quitSafely();
            } else {
                mBackgroundHandler.getLooper().quit();
            }
            mBackgroundHandler = null;
        }
        if (mCountDownTimer != null && !mCountDownTimer.isStop()) {
            mCountDownTimer.cancelTimer();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (permissions.length != 2 || grantResults.length != 2) {
                    throw new RuntimeException("Error on requesting camera permission.");
                }
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.camera_permission_not_granted,
                            Toast.LENGTH_SHORT).show();
                }
                if (grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.storage_permission_not_granted,
                            Toast.LENGTH_SHORT).show();
                }
                // No need to start camera here; it is handled by onResume
                break;
            default:
                break;
        }
    }

    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }

    private CameraView.Callback mCallback
            = new CameraView.Callback() {

        @Override
        public void onCameraOpened(CameraView cameraView) {
            Log.d(TAG, "onCameraOpened");
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            Log.d(TAG, "onCameraClosed");
        }

        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {
            Log.d(TAG, "onPictureTaken " + data.length);

            mCountDownTimer = new CountDownTimer(new ReFreshHandler(MainActivity.this),
                    countTime);
            mCountDownTimer.start();

            getBackgroundHandler().post(new Runnable() {
                @Override
                public void run() {

                    FileOutputStream fos = null;
                    try {
                        File file = new File(
                                Environment.getExternalStorageDirectory().getAbsolutePath()
                                        + "/LiteTrace1");
                        if (!file.exists()) {
                            file.mkdirs();
                        }
                        File[] files = file.listFiles(new FileFilter() {
                            @Override
                            public boolean accept(File pathname) {
                                return pathname.isDirectory();
                            }
                        });
                        if (files.length > 0) {
                            File lastFile = files[files.length - 1];
                            if (lastFile.isDirectory()) {
                                if (lastFile.list().length >= countPic) {
                                    Log.d("lxd", "超过" + countPic + "张，再建一个文件夹");
                                    String fileName = getCurrentDateTime("yyyyMMdd-HHmmss");
                                    file = new File(
                                            file.getAbsolutePath() + File.separator + fileName);
                                    if (!file.exists()) {
                                        file.mkdirs();
                                    }
                                } else {
                                    file = lastFile;
                                    Log.d("lxd", "没超过" + countPic + "张，继续用");
                                }
                            }
                        } else {
                            Log.d("lxd", "没有文件夹");
                            String fileName = getCurrentDateTime("yyyyMMdd-HHmmss");
                            file = new File(
                                    file.getAbsolutePath() + File.separator + fileName);
                            if (!file.exists()) {
                                file.mkdirs();
                            }
                        }
                        File file2 = new File(file, getCurrentDateTime("MMdd-HHmmss") + ".jpg");
                        fos = new FileOutputStream(file2);
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        //缩放照片分辨率倍数（2的倍数，这里写3，实际是2）
                        options.inSampleSize = 3;
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length,
                                options);
                        Matrix matrix = new Matrix();
                        //旋转角度，保证横向拍照时保存的图片方向是对的
                        matrix.setRotate(270);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        //压缩图片质量
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                        fos.flush();
                        fos.close();
                        //发送广播让图片显示到系统图库
                        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        Uri uri = Uri.fromFile(file2);
                        intent.setData(uri);
                        sendBroadcast(intent);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (fos != null) {
                            try {
                                fos.flush();
                                fos.close();
                            } catch (IOException e) {
                                // Ignore
                            }
                        }
                    }
                }
            });
        }
    };

    public static String getCurrentDateTime(String format) {
        if (format == null) {
            format = "yyyy-MM-dd-HH-mm-ss";
        }
        SimpleDateFormat formatter = new SimpleDateFormat(format, Locale.CHINA);
        Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
        return formatter.format(curDate);
    }

    public static class ConfirmationDialogFragment extends DialogFragment {

        private static final String ARG_MESSAGE = "message";
        private static final String ARG_PERMISSIONS = "permissions";
        private static final String ARG_REQUEST_CODE = "request_code";
        private static final String ARG_NOT_GRANTED_MESSAGE = "not_granted_message";

        public static ConfirmationDialogFragment newInstance(@StringRes int message,
                String[] permissions, int requestCode, @StringRes int notGrantedMessage) {
            ConfirmationDialogFragment fragment = new ConfirmationDialogFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_MESSAGE, message);
            args.putStringArray(ARG_PERMISSIONS, permissions);
            args.putInt(ARG_REQUEST_CODE, requestCode);
            args.putInt(ARG_NOT_GRANTED_MESSAGE, notGrantedMessage);
            fragment.setArguments(args);
            return fragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle args = getArguments();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(args.getInt(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String[] permissions = args.getStringArray(ARG_PERMISSIONS);
                                    if (permissions == null) {
                                        throw new IllegalArgumentException();
                                    }
                                    ActivityCompat.requestPermissions(getActivity(),
                                            permissions, args.getInt(ARG_REQUEST_CODE));
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(getActivity(),
                                            args.getInt(ARG_NOT_GRANTED_MESSAGE),
                                            Toast.LENGTH_SHORT).show();
                                }
                            })
                    .create();
        }

    }

    private static class ReFreshHandler extends Handler {
        WeakReference<MainActivity> mWeakReference;

        ReFreshHandler(MainActivity activity) {
            this.mWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            final MainActivity activity = mWeakReference.get();
            if (activity == null) {
                return;
            }
            activity.tv_time.setText(msg.what + "");
            if (msg.what == 0) {
                if (activity.mCameraView != null) {
                    activity.mCameraView.takePicture();
                    activity.playSound();
                }
            }
        }
    }

    /**
     * 播放系统拍照声音
     */
    public void playSound() {
        MediaPlayer mediaPlayer = null;
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int volume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);

        if (volume != 0) {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this,
                        Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));
            }
            if (mediaPlayer != null) {
                mediaPlayer.start();
            }
        }
    }
}
