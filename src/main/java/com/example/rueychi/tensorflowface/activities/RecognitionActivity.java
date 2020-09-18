package com.example.rueychi.tensorflowface.activities;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rueychi.tensorflowface.R;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.File;
import java.util.List;
import java.util.Locale;

import ch.zhaw.facerecognitionlibrary.Helpers.CustomCameraView;
import ch.zhaw.facerecognitionlibrary.Helpers.FileHelper;
import ch.zhaw.facerecognitionlibrary.PreProcessor.PreProcessorFactory;
import ch.zhaw.facerecognitionlibrary.Recognition.Recognition;
import ch.zhaw.facerecognitionlibrary.Recognition.RecognitionFactory;

//import ch.zhaw.facerecognitionlibrary.Helpers.MatOperation;

public class RecognitionActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2,TextToSpeech.OnInitListener {
    private CustomCameraView mRecognitionView;
    private static final String TAG = "Recognition";
    private FileHelper fh;
    private Recognition rec;
    private PreProcessorFactory ppF;
    private ProgressBar progressBar;
    private boolean front_camera;
    private boolean night_portrait;
    private int exposure_compensation;
    private TextToSpeech tts;
    String name="";

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        tts = new TextToSpeech(this, this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.recognition_layout);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        fh = new FileHelper();
        File folder = new File(fh.getFolderPath());
        if (folder.mkdir() || folder.isDirectory()) {
            Log.i(TAG, "New directory for photos created");
        } else {
            Log.i(TAG, "Photos directory already existing");
        }
        mRecognitionView = (CustomCameraView) findViewById(R.id.RecognitionView);
        // Use camera which is selected in settings
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        front_camera = sharedPref.getBoolean("key_front_camera", true);
        night_portrait = sharedPref.getBoolean("key_night_portrait", false);
        exposure_compensation = Integer.valueOf(sharedPref.getString("key_exposure_compensation", "20"));

        if (front_camera) {
            mRecognitionView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        } else {
            mRecognitionView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
        }
        mRecognitionView.setVisibility(SurfaceView.VISIBLE);
        mRecognitionView.setCvCameraViewListener(this);

        int maxCameraViewWidth = Integer.parseInt(sharedPref.getString("key_maximum_camera_view_width", "640"));
        int maxCameraViewHeight = Integer.parseInt(sharedPref.getString("key_maximum_camera_view_height", "480"));
        mRecognitionView.setMaxFrameSize(maxCameraViewWidth, maxCameraViewHeight);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRecognitionView != null)
            mRecognitionView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mRecognitionView != null)
            mRecognitionView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {

        if (night_portrait) {
            mRecognitionView.setNightPortrait();
        }

        if (exposure_compensation != 50 && 0 <= exposure_compensation && exposure_compensation <= 100)
            mRecognitionView.setExposure(exposure_compensation);
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat imgRgba = inputFrame.rgba();
        Mat img = new Mat();
        imgRgba.copyTo(img);
        List<Mat> images = ppF.getProcessedImage(img, PreProcessorFactory.PreprocessingMode.RECOGNITION);
        Rect[] faces = ppF.getFacesForRecognition();

        // Selfie / Mirror mode
        if (front_camera) {
            Core.flip(imgRgba, imgRgba, 1);
        }
        if (images == null || images.size() == 0 || faces == null || faces.length == 0 || !(images.size() == faces.length)) {
            // skip
            return imgRgba;
        } else {
            faces = MyMatOperation.rotateFaces(imgRgba, faces, ppF.getAngleForRecognition());
            for (int i = 0; i < faces.length; i++) {
                // 這行產生綠框與名稱
                // rec.recognize(images.get(i), "")就是人名
                Log.i("MAN", rec.recognize(images.get(i), ""));
                //MyMatOperation.drawRectangleAndLabelOnPreview(imgRgba, faces[i], rec.recognize(images.get(i), ""), front_camera);
                name=rec.recognize(images.get(i), "");
                runOnUiThread(new Runnable() {
                    public void run() {
                        //Toast.makeText(RecognitionActivity.this,name,Toast.LENGTH_LONG).show();
                        Toast toast = Toast.makeText(RecognitionActivity.this, name, Toast.LENGTH_SHORT);
                        LinearLayout toastLayout = (LinearLayout) toast.getView();
                        TextView toastTV = (TextView) toastLayout.getChildAt(0);
                        toastTV.setTextSize(40);
                        toastTV.setTextColor(Color.GREEN);
                        toast.show();
                    }
                });
                speakOut();
                // 先取消，因為無法顯示中文
                //MyMatOperation.drawRectangleAndLabelOnPreview(imgRgba, faces[i], "測試", front_camera);
                MyMatOperation.drawRectangleAndLabelOnPreview(imgRgba, faces[i], "", front_camera);
            }
            return imgRgba;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        ppF = new PreProcessorFactory(getApplicationContext());

        final android.os.Handler handler = new android.os.Handler(Looper.getMainLooper());
        Thread t = new Thread(new Runnable() {
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                });
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String algorithm = sharedPref.getString("key_classification_method", getResources().getString(R.string.eigenfaces));
                rec = RecognitionFactory.getRecognitionAlgorithm(getApplicationContext(), Recognition.RECOGNITION, algorithm);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }
        });

        t.start();

        // Wait until Eigenfaces loading thread has finished
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mRecognitionView.enableView();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.TAIWAN);
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Language is not supported");
            } else {
                speakOut();
            }
        } else {
            Log.e("TTS", "Initilization Failed");
        }
    }
    private void speakOut() {
        String text = name.toString();  //辨識到的名字
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

}
