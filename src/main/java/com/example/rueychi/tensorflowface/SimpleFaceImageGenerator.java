package com.example.rueychi.tensorflowface;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.rueychi.tensorflowface.activities.MyMatOperation;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.File;
import java.util.List;

import ch.zhaw.facerecognitionlibrary.Helpers.FileHelper;
import ch.zhaw.facerecognitionlibrary.Helpers.MatName;
import ch.zhaw.facerecognitionlibrary.PreProcessor.PreProcessorFactory;

public class SimpleFaceImageGenerator {
    private PreProcessorFactory ppF;
    private FileHelper fh;
    private @Nullable String name;
    private int total;

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    SimpleFaceImageGenerator(Context context, @Nullable String name) {
        this.name = name;
        this.fh = new FileHelper();
        this.total = 0;
        this.ppF = new PreProcessorFactory(context);
    }

    public Bitmap onBitmap(Bitmap bitmap) {
        boolean imageModified = false;
        Mat imgRgba = new Mat();
        Utils.bitmapToMat(bitmap, imgRgba);
        Mat imgCopy = new Mat();
        imgRgba.copyTo(imgCopy);
        // Check that only 1 face is found. Skip if any or more than 1 are found.
        List<Mat> images = null;
        try {
            images = ppF.getCroppedImage(imgCopy);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (images != null && images.size() == 1) {
            Mat img = images.get(0);
            if (img != null) {
                Rect[] faces = ppF.getFacesForRecognition();
                //Only proceed if 1 face has been detected, ignore if 0 or more than 1 face have been detected
                if ((faces != null) && (faces.length == 1)) {
                    // 畫出綠框
                    faces = MyMatOperation.rotateFaces(imgRgba, faces, ppF.getAngleForRecognition());
                    if(SimpleFaceImageGenerator.this.name!=null) {
                        MatName m = new MatName(SimpleFaceImageGenerator.this.name + "_" + total, img);
                        String wholeFolderPath = FileHelper.TRAINING_PATH + SimpleFaceImageGenerator.this.name;
                        Log.d("Debug", "wholeFolderPath = " + wholeFolderPath);
                        File personFolder = new File(wholeFolderPath);
                        personFolder.mkdir();
                        @SuppressWarnings("unused")
                        String faceImagePath = fh.saveMatToImage(m, wholeFolderPath + "/");
                    }
                    for (Rect face : faces) {
                        // 畫出綠框
                        imageModified = true;
                        MyMatOperation.drawRectangleAndLabelOnPreview(imgRgba, face, String.valueOf(total), false); // TODO flip?
                    }
                    total++;
                }
            }
        }
        if (imageModified) {
            bitmap = Bitmap.createBitmap(bitmap.getWidth(),bitmap.getHeight(),bitmap.getConfig());
            Utils.matToBitmap(imgRgba, bitmap);
        }
        return bitmap;
    }

    public int getRecognizedImageCount() {
        return this.total;
    }
}