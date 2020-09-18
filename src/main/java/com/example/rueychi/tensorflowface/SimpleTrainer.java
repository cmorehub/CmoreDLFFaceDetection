package com.example.rueychi.tensorflowface;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.List;

import ch.zhaw.facerecognitionlibrary.Helpers.FileHelper;
import ch.zhaw.facerecognitionlibrary.Helpers.MatName;
import ch.zhaw.facerecognitionlibrary.Helpers.PreferencesHelper;
import ch.zhaw.facerecognitionlibrary.PreProcessor.PreProcessorFactory;
import ch.zhaw.facerecognitionlibrary.Recognition.Recognition;
import ch.zhaw.facerecognitionlibrary.Recognition.RecognitionFactory;

public class SimpleTrainer {
    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    interface Callback {
        void onProgress(String name, int total, int progress);

        void onFinished(boolean success);
    }

    public static class TrainProgress {
        final String name;
        final int progress, total;

        TrainProgress(String name, int progress, int total) {
            this.name = name;
            this.progress = progress;
            this.total = total;
        }
    }

    public static AsyncTask train(final Context context, final Callback callback, final boolean wholeImage) {
        return new AsyncTask<Object, TrainProgress, Boolean>() {
            ProgressDialog trainProgressDialog;
            @Override
            protected void onPreExecute() {
                this.trainProgressDialog = ProgressDialog.show(context, "Train", "Preparing...", true, false);
                this.trainProgressDialog.setCanceledOnTouchOutside(false);
            }

            @Override
            protected Boolean doInBackground(Object... params) {
                PreProcessorFactory ppF = new PreProcessorFactory(context.getApplicationContext());
                PreferencesHelper preferencesHelper = new PreferencesHelper(context.getApplicationContext());
                String algorithm = preferencesHelper.getClassificationMethod();

                FileHelper fileHelper = new FileHelper();
                fileHelper.createDataFolderIfNotExsiting();
                final File[] persons = fileHelper.getTrainingList();
                if (persons.length > 0) {
                    Recognition rec = RecognitionFactory.getRecognitionAlgorithm(context.getApplicationContext(), Recognition.TRAINING, algorithm);
                    for (File person : persons) {
                        if (person.isDirectory()) {
                            File[] files = person.listFiles();
                            int counter = 1;
                            for (File file : files) {
                                if (FileHelper.isFileAnImage(file)) {
                                    Mat imgRgb = Imgcodecs.imread(file.getAbsolutePath());
                                    Imgproc.cvtColor(imgRgb, imgRgb, Imgproc.COLOR_BGRA2RGBA);
                                    Mat processedImage = new Mat();
                                    imgRgb.copyTo(processedImage);
                                    if(!wholeImage) {
                                        List<Mat> images = ppF.getProcessedImage(processedImage, PreProcessorFactory.PreprocessingMode.RECOGNITION);
                                        if (images == null || images.size() > 1) {
                                            // More than 1 face detected --> cannot use this file for training
                                            continue;
                                        } else {
                                            processedImage = images.get(0);
                                        }
                                        if (processedImage.empty()) {
                                            continue;
                                        }
                                    }
                                    // The last token is the name --> Folder name = Person name
                                    String[] tokens = file.getParent().split("/");
                                    final String name = tokens[tokens.length - 1];

                                    MatName m = new MatName("processedImage", processedImage);
                                    fileHelper.saveMatToImage(m, FileHelper.DATA_PATH);

                                    rec.addImage(processedImage, name, false);

//                                      fileHelper.saveCroppedImage(imgRgb, ppF, file, name, counter);

                                    // Update screen to show the progress
                                    this.publishProgress(new TrainProgress(name, counter, files.length));
                                    counter++;
                                }
                            }
                        }
                    }
                    return rec.train();
                } else {
                    return true;
                }
            }

            @Override
            protected void onCancelled() {
                callback.onFinished(false);
            }

            @Override
            protected void onProgressUpdate(TrainProgress... progresses) {
                TrainProgress lastTrainProgress = null;
                for (TrainProgress progress : progresses) {
                    lastTrainProgress = progress;
                    callback.onProgress(progress.name, progress.total, progress.progress);
                }
                if(lastTrainProgress!=null) {
                    this.trainProgressDialog.setMessage(lastTrainProgress.name+"("+lastTrainProgress.progress+"/"+lastTrainProgress.total+" trained)");
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                callback.onFinished(success);
                this.trainProgressDialog.dismiss();
            }
        };
    }
}
