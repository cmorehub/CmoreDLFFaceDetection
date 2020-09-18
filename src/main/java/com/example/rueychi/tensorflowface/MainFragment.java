package com.example.rueychi.tensorflowface;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.rueychi.tensorflowface.R;
import com.example.rueychi.tensorflowface.activities.AddPersonActivity;
import com.example.rueychi.tensorflowface.activities.RecognitionActivity;
import com.example.rueychi.tensorflowface.activities.SettingsActivity;
import com.example.rueychi.tensorflowface.activities.TrainingActivity;

import java.io.File;

import ch.zhaw.facerecognitionlibrary.Helpers.FileHelper;

public class MainFragment extends android.support.v4.app.Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.tensor_face_main,container,false);
        double accuracy = getArguments()==null?0:getArguments().getDouble("accuracy", 0);
        if (accuracy != 0){
            Toast.makeText(getContext().getApplicationContext(), "The accuracy was " + accuracy * 100 + " %", Toast.LENGTH_LONG).show();
            getArguments().remove("accuracy");
        }

        //PreferenceManager.setDefaultValues(MainActivity.this, R.xml.preferences, false);
        PreferenceManager.setDefaultValues(getContext(), R.xml.mysetting, false);

        Button callSettings = (Button)contentView.findViewById(R.id.button_settings);
        callSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 設定
                startActivity(new Intent(v.getContext(), SettingsActivity.class));
            }
        });

        Button callAddPerson = (Button)contentView.findViewById(R.id.button_addPerson);
        callAddPerson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 加入人員
                startActivity(new Intent(v.getContext(), AddPersonActivity.class));
            }
        });

        FileHelper fh = new FileHelper();

        Button callRecognition = (Button)contentView.findViewById(R.id.button_recognition_view);
        if(!((new File(FileHelper.DATA_PATH)).exists())) callRecognition.setEnabled(false);
        callRecognition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 開始辨識
                startActivity(new Intent(v.getContext(), RecognitionActivity.class));
            }
        });


        Button callTraining = (Button)contentView.findViewById(R.id.button_recognition_training);
        if(fh.getTrainingList().length == 0) callTraining.setEnabled(false);
        callTraining.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 訓練
                startActivity(new Intent(v.getContext(), TrainingActivity.class));
            }
        });

        return contentView;
    }
}
