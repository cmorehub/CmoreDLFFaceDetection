package com.example.rueychi.tensorflowface.activities;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.rueychi.tensorflowface.R;

import java.io.File;

import ch.zhaw.facerecognitionlibrary.Helpers.FileHelper;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tensor_face_main);
        Intent intent = getIntent();
        String training = intent.getStringExtra("training");
        if (training != null && !training.isEmpty()){
            Toast.makeText(getApplicationContext(), training, Toast.LENGTH_SHORT).show();
            intent.removeExtra("training");
        }

        double accuracy = intent.getDoubleExtra("accuracy", 0);
        if (accuracy != 0){
            Toast.makeText(getApplicationContext(), "The accuracy was " + accuracy * 100 + " %", Toast.LENGTH_LONG).show();
            intent.removeExtra("accuracy");
        }

        //PreferenceManager.setDefaultValues(MainActivity.this, R.xml.preferences, false);
        PreferenceManager.setDefaultValues(MainActivity.this, R.xml.mysetting, false);

        Button callSettings = (Button)findViewById(R.id.button_settings);
        callSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 設定
                startActivity(new Intent(v.getContext(), SettingsActivity.class));
            }
        });

        Button callAddPerson = (Button)findViewById(R.id.button_addPerson);
        callAddPerson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 加入人員
                startActivity(new Intent(v.getContext(), AddPersonActivity.class));
            }
        });

        FileHelper fh = new FileHelper();

        Button callRecognition = (Button)findViewById(R.id.button_recognition_view);
        if(!((new File(fh.DATA_PATH)).exists())) callRecognition.setEnabled(false);
        callRecognition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 開始辨識
                startActivity(new Intent(v.getContext(), RecognitionActivity.class));
            }
        });


        Button callTraining = (Button)findViewById(R.id.button_recognition_training);
        if(fh.getTrainingList().length == 0) callTraining.setEnabled(false);
        callTraining.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 訓練
                startActivity(new Intent(v.getContext(), TrainingActivity.class));
            }
        });
    }
}
