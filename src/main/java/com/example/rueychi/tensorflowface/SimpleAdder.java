package com.example.rueychi.tensorflowface;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.example.rueychi.tensorflowface.activities.MyMatOperation;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.File;
import java.util.Date;
import java.util.List;

import ch.zhaw.facerecognitionlibrary.Helpers.CustomCameraView;
import ch.zhaw.facerecognitionlibrary.Helpers.FileHelper;
import ch.zhaw.facerecognitionlibrary.Helpers.MatName;
import ch.zhaw.facerecognitionlibrary.PreProcessor.PreProcessorFactory;

public class SimpleAdder implements CameraBridgeViewBase.CvCameraViewListener2 {
    private CustomCameraView addPersonView;
    // The timerDiff defines after how many milliseconds a picture is taken
    private long timerDiff;
    private long lastTime;
    private PreProcessorFactory ppF;
    private FileHelper fh;
    private String name;
    private int total;
    private int numberOfPictures;
    private boolean capturePressed;
    private boolean front_camera;
    private boolean night_portrait;
    private int exposure_compensation;

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    interface Callback {
        void onFinished();
    }

    private final Context context;
    private View contentView;
    private final boolean auto;
    private final Callback callback;

    SimpleAdder(Context context, Callback callback, String name, boolean auto) {
        this.context = context;
        this.callback = callback;
        this.auto = auto;
        this.name = name;
        this.contentView = new FrameLayout(context);
        this.contentView = LayoutInflater.from(context).inflate(R.layout.activity_add_person_preview, (FrameLayout) this.contentView);
        this.capturePressed = false;

        this.fh = new FileHelper();
        this.total = 0;
        this.lastTime = new Date().getTime();

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        this.timerDiff = Integer.valueOf(sharedPrefs.getString("key_timerDiff", "500"));

        this.addPersonView = (CustomCameraView) this.contentView.findViewById(R.id.AddPersonPreview);
    }

    private void setUpAddPersonView() {
        this.ppF = new PreProcessorFactory(context);

        if (!this.auto) {
            ImageButton btn_Capture = (ImageButton) this.contentView.findViewById(R.id.btn_Capture);
            btn_Capture.setVisibility(View.VISIBLE);
            btn_Capture.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    capturePressed = true;
                }
            });
        }
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        this.front_camera = sharedPref.getBoolean("key_front_camera", true);

        this.numberOfPictures = Integer.valueOf(sharedPref.getString("key_numberOfPictures", "100"));

        this.night_portrait = sharedPref.getBoolean("key_night_portrait", false);
        this.exposure_compensation = Integer.valueOf(sharedPref.getString("key_exposure_compensation", "50"));

        if (this.front_camera) {
            this.addPersonView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        } else {
            this.addPersonView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
        }
        this.addPersonView.setVisibility(SurfaceView.VISIBLE);
        this.addPersonView.setCvCameraViewListener(this);

        int maxCameraViewWidth = Integer.parseInt(sharedPref.getString("key_maximum_camera_view_width", "640"));
        int maxCameraViewHeight = Integer.parseInt(sharedPref.getString("key_maximum_camera_view_height", "480"));
        this.addPersonView.setMaxFrameSize(maxCameraViewWidth, maxCameraViewHeight);
        this.addPersonView.enableView();
    }

    public AlertDialog openAddPersonDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this.context)
                .setView(this.contentView)
                .setTitle("Recognize")
                .setNegativeButton("Close", null)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if(addPersonView.isEnabled()){
                            addPersonView.disableView();
                        }
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if(addPersonView.isEnabled()){
                            addPersonView.disableView();
                        }
                    }
                });
        this.setUpAddPersonView();
        return dialogBuilder.show();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        if (night_portrait) {
            addPersonView.setNightPortrait();
        }

        if (exposure_compensation != 50 && 0 <= exposure_compensation && exposure_compensation <= 100)
            addPersonView.setExposure(exposure_compensation);
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat imgRgba = inputFrame.rgba();
        Mat imgCopy = new Mat();
        imgRgba.copyTo(imgCopy);
        // Selfie / Mirror mode
        if (front_camera) {
            Core.flip(imgRgba, imgRgba, 1);
        }

        long time = new Date().getTime();
        if (!this.auto||lastTime + timerDiff < time) {
            lastTime = time;
            // Check that only 1 face is found. Skip if any or more than 1 are found.
            List<Mat> images = ppF.getCroppedImage(imgCopy);
            if (images != null && images.size() == 1) {
                Mat img = images.get(0);
                if (img != null) {
                    Rect[] faces = ppF.getFacesForRecognition();
                    //Only proceed if 1 face has been detected, ignore if 0 or more than 1 face have been detected
                    if ((faces != null) && (faces.length == 1)) {
                        // 畫出綠框
                        faces = MyMatOperation.rotateFaces(imgRgba, faces, ppF.getAngleForRecognition());
                        if (this.auto || capturePressed) {
                            MatName m = new MatName(name + "_" + total, img);
                            String wholeFolderPath = FileHelper.TRAINING_PATH + name;
                            Log.d("Debug", "wholeFolderPath = "+wholeFolderPath);
                            new File(wholeFolderPath).mkdirs();
                            fh.saveMatToImage(m, wholeFolderPath + "/");
                            for (Rect face : faces) {
                                // 畫出綠框
                                MyMatOperation.drawRectangleAndLabelOnPreview(imgRgba, face, String.valueOf(total), front_camera);
                            }

                            total++;

                            // Stop after numberOfPictures (settings option)
                            if (total >= numberOfPictures) {
                                this.callback.onFinished();
                            }
                            capturePressed = false;
                        } else {
                            for (Rect face : faces) {
                                // 畫出綠框
                                MyMatOperation.drawRectangleOnPreview(imgRgba, face, front_camera);
                            }
                        }
                    }
                }
            }
        }

        return imgRgba;
    }

}
