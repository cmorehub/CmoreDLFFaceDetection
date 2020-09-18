package com.example.rueychi.tensorflowface;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.example.rueychi.tensorflowface.activities.MyMatOperation;

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

public class SimpleRecognizer implements CameraBridgeViewBase.CvCameraViewListener2, TextToSpeech.OnInitListener {
    private CustomCameraView recognitionView;
    private static final String TAG = "Recognition";
    private Recognition rec;
    private PreProcessorFactory ppF;
    private ProgressBar progressBar;
    private boolean front_camera;
    private boolean night_portrait;
    private int exposure_compensation;
    private TextToSpeech tts;
    private String name = "";

    private final Context context;
    private final Callback callback;
    private View contentView;

    private final boolean wholeImage;

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    interface Callback {
        void onPersonRecognized(String name);
    }

    SimpleRecognizer(Context context, Callback callback, boolean wholeImage) {
        this.context = context;
        this.callback = callback;
        this.wholeImage = wholeImage;
        this.tts = new TextToSpeech(context, this);
        this.contentView = new FrameLayout(context);
        this.contentView = LayoutInflater.from(context).inflate(R.layout.recognition_layout, (FrameLayout) this.contentView);
        this.progressBar = (ProgressBar) this.contentView.findViewById(R.id.progressBar);
        File folder = new File(FileHelper.getFolderPath());
        if (folder.mkdir() || folder.isDirectory()) {
            Log.i(TAG, "New directory for photos created");
        } else {
            Log.i(TAG, "Photos directory already existing");
        }
        this.recognitionView = (CustomCameraView) this.contentView.findViewById(R.id.RecognitionView);
    }

    public void onCameraViewStarted(int width, int height) {
        if (this.night_portrait) {
            this.recognitionView.setNightPortrait();
        }

        if (this.exposure_compensation != 50 && 0 <= this.exposure_compensation && this.exposure_compensation <= 100)
            this.recognitionView.setExposure(this.exposure_compensation);
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat imgRgba = inputFrame.rgba();
        Mat img = new Mat();
        imgRgba.copyTo(img);
        if(!wholeImage) {
            List<Mat> images = this.ppF.getProcessedImage(img, PreProcessorFactory.PreprocessingMode.RECOGNITION);
            Rect[] faces = this.ppF.getFacesForRecognition();

            // Selfie / Mirror mode
            if (this.front_camera) {
                Core.flip(imgRgba, imgRgba, 1);
            }
            if (images == null || images.size() == 0 || faces == null || faces.length == 0 || !(images.size() == faces.length)) {
                // skip
                return imgRgba;
            } else {
            faces = MyMatOperation.rotateFaces(imgRgba, faces, this.ppF.getAngleForRecognition());
            for (int i = 0; i < faces.length; i++) {
                // 這行產生綠框與名稱
//                 rec.recognize(images.get(i), "")就是人名
                Log.i("MAN", this.rec.recognize(images.get(i), ""));
//                MyMatOperation.drawRectangleAndLabelOnPreview(imgRgba, faces[i], rec.recognize(images.get(i), ""), front_camera);
                this.name = this.rec.recognize(images.get(i), "");
                this.callback.onPersonRecognized(this.name);
                speakOut();
                // 先取消，因為無法顯示中文
//                MyMatOperation.drawRectangleAndLabelOnPreview(imgRgba, faces[i], "測試", front_camera);
                MyMatOperation.drawRectangleAndLabelOnPreview(imgRgba, faces[i], "", this.front_camera);
            }
                return imgRgba;
            }
        } else{
            // Selfie / Mirror mode
            if (this.front_camera) {
                Core.flip(imgRgba, imgRgba, 1);
            }
            this.name = this.rec.recognize(img, "");
            if(this.name!=null&&!this.name.equals("")) {
                this.callback.onPersonRecognized(this.name);
                speakOut();
            }
            return imgRgba;
        }
    }

    private void setUpRecognizerView() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        this.front_camera = sharedPref.getBoolean("key_front_camera", true);
        this.night_portrait = sharedPref.getBoolean("key_night_portrait", false);
        this.exposure_compensation = Integer.valueOf(sharedPref.getString("key_exposure_compensation", "20"));

        if (this.front_camera) {
            this.recognitionView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        } else {
            this.recognitionView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
        }

        this.recognitionView.setVisibility(SurfaceView.VISIBLE);
        this.recognitionView.setCvCameraViewListener(this);

        int maxCameraViewWidth = Integer.parseInt(sharedPref.getString("key_maximum_camera_view_width", "640"));
        int maxCameraViewHeight = Integer.parseInt(sharedPref.getString("key_maximum_camera_view_height", "480"));
        this.recognitionView.setMaxFrameSize(maxCameraViewWidth, maxCameraViewHeight);

        this.ppF = new PreProcessorFactory(this.context.getApplicationContext());

        // TODO 這裡應該要改一下...
        final android.os.Handler handler = new android.os.Handler(Looper.getMainLooper());
        Thread t = new Thread(new Runnable() {
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                });
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
                String algorithm = sharedPref.getString("key_classification_method", context.getResources().getString(R.string.eigenfaces));
                rec = RecognitionFactory.getRecognitionAlgorithm(context.getApplicationContext(), Recognition.RECOGNITION, algorithm);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }
        });

        t.start();

        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        recognitionView.enableView();
    }

    public void startRecognize() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this.context)
                .setView(this.contentView)
                .setTitle("Recognize")
                .setNegativeButton("Close", null)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if(recognitionView.isEnabled()){
                            recognitionView.disableView();
                        }
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if(recognitionView.isEnabled()){
                            recognitionView.disableView();
                        }
                    }
                });
        this.setUpRecognizerView();
        dialogBuilder.show();
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
        String text = name;  //辨識到的名字
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

}
