package com.example.rueychi.tensorflowface;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.evermore.cmoredlf.library.CmoreDLF;
import com.example.rueychi.tensorflowface.activities.SettingsActivity;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;

import ch.zhaw.facerecognitionlibrary.Helpers.FileHelper;

public class MainDLF implements CmoreDLF {

    private AppInterface appInterface;

    @Override
    public void onCreate(AppInterface appInterface) {
        this.appInterface = appInterface;
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public AsyncTask invoke(String method, final String parameters) {
        switch (method) {
            case "go":
                return new AsyncTask() {
                    @Override
                    protected Object doInBackground(Object[] params) {
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Object o) {
                        appInterface.getOutput().requestNewActivity(new MainFragment());
                    }
                };
            case "add":
                return new AsyncTask() {
                    @Override
                    protected Object doInBackground(Object[] params) {
                        return null;
                    }

                    private boolean personAlreadyExists(File[] folderList, String personName) {
                        if (folderList != null && folderList.length > 0) {
                            for (File personFolder : folderList) {
                                String[] tokens = personFolder.getAbsolutePath().split("/");
                                final String folderNAme = tokens[tokens.length - 1];
                                if (folderNAme.equals(personName)) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    }

                    @Override
                    protected void onPostExecute(Object o) {
                        final EditText nameEditText = new EditText(appInterface.getContext());
                        new AlertDialog.Builder(appInterface.getContext())
                                .setTitle("Set Person Name")
                                .setView(nameEditText)
                                .setPositiveButton("Start", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String name = nameEditText.getText().toString();
                                        if (personAlreadyExists(new FileHelper().getTrainingList(), name)) {
                                            Toast.makeText(appInterface.getContext(), "This name is already used. Please choose another one.", Toast.LENGTH_SHORT).show();
                                        } else {
                                            final AlertDialog[] alertDialog = new AlertDialog[1];
                                            alertDialog[0] = new SimpleAdder(appInterface.getContext(), new SimpleAdder.Callback() {
                                                @Override
                                                public void onFinished() {
                                                    alertDialog[0].dismiss();
                                                }
                                            }, name, true).openAddPersonDialog();
                                        }
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                };
            case "recognize":
                return new AsyncTask() {
                    @Override
                    protected Object doInBackground(Object[] params) {
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Object o) {
                        boolean wholeImage = false;
                        try {
                            wholeImage = Boolean.valueOf(parameters);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        new SimpleRecognizer(appInterface.getContext(), new SimpleRecognizer.Callback() {
                            @Override
                            public void onPersonRecognized(String name) {
                                appInterface.getOutput().writeDataValue("recognize_person", name);
                            }
                        },wholeImage).startRecognize();
                    }
                };
            case "train":
                boolean wholeImage = false;
                try {
                    wholeImage = Boolean.valueOf(parameters);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return SimpleTrainer.train(this.appInterface.getContext(), new SimpleTrainer.Callback() {
                    @Override
                    public void onProgress(String name, int total, int progress) {
//                        appInterface.getOutput().writeDataValue("progress","Training "+name+" "+progress+"/"+total);
                    }

                    @Override
                    public void onFinished(boolean success) {
                        appInterface.getOutput().writeDataValue("finished", success ? "1" : "0");
                    }
                },wholeImage);
            case "recognize_and_train":
                return null;
            case "set":
                return new AsyncTask() {
                    @Override
                    protected Object doInBackground(Object[] params) {
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Object o) {
                        appInterface.getContext().startActivity(new Intent(appInterface.getContext(), SettingsActivity.class));
                    }
                };
            default:
                return null;
        }
    }

    private AlertDialog currentStreamDialog;
    private ImageView streamImageView, recognizeImageView;
    private SimpleFaceImageGenerator currentFaceImageGenerator;

    private ChannelIn channelIn = new ChannelIn() {
        @Override
        public void onInputStreamReceived(HashMap<String, String> metadata, final InputStream inputStream) {
            // TODO metadata?
            Log.d("Recognizer", "onInputStreamReceived");
            if (currentFaceImageGenerator == null) {
                currentFaceImageGenerator = new SimpleFaceImageGenerator(appInterface.getContext(), null);
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final Bitmap streamBitmap = BitmapFactory.decodeStream(inputStream);
                    Log.d("Recognizer", "Handle Bitmap = " + (streamBitmap == null ? "null" : ("width=" + streamBitmap.getWidth() + ",height=" + streamBitmap.getHeight())));
                    if (streamBitmap != null) {
                        // TODO
                        final Bitmap recognizedBitmap = currentFaceImageGenerator.onBitmap(streamBitmap);
                        new Handler(appInterface.getContext().getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (currentStreamDialog == null) {
                                    LinearLayout imagesLayout = new LinearLayout(appInterface.getContext());
                                    imagesLayout.setOrientation(LinearLayout.VERTICAL);
                                    streamImageView = new ImageView(appInterface.getContext());
                                    recognizeImageView = new ImageView(appInterface.getContext());
                                    imagesLayout.addView(streamImageView);
                                    imagesLayout.addView(recognizeImageView);
                                    currentStreamDialog = new AlertDialog.Builder(appInterface.getContext())
                                            .setView(imagesLayout)
                                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    currentStreamDialog = null;
                                                }
                                            })
                                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                                @Override
                                                public void onCancel(DialogInterface dialog) {
                                                    currentStreamDialog = null;
                                                }
                                            })
                                            .show();
                                }
                                streamImageView.setImageBitmap(streamBitmap);
                                if (recognizedBitmap != streamBitmap) {
                                    recognizeImageView.setImageBitmap(recognizedBitmap);
                                }
                            }
                        });
                    }
                }
            }).start();
        }
    };

    @Override
    public ChannelIn onReceiveFromChannel() {
        return this.channelIn;
    }
}
