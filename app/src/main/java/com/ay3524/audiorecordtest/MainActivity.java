package com.ay3524.audiorecordtest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.UploadProgressListener;

import org.json.JSONObject;

import java.io.File;

public class MainActivity extends AppCompatActivity implements WavRecorder.RecordingListener {

    private static final int RECORD_AUDIO_REQUEST_CODE = 100;
    private static final String TAG = MainActivity.class.getSimpleName();
    //TODO change upload url
    private static final String UPLOAD_URL = "";
    private static String audioPlayerName = null;

    private WavRecorder wavRecorder;
    private Button playButton;
    private Button stopButton;
    private Button recordStartButton;
    private Button recordStopButton;
    private Button sendStopButton;
    TextView textView;

    private ProgressBar progressBar;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getPermissionToRecordAudio();
        setUpViews();
    }

    private void setUpViews() {
        recordStartButton = findViewById(R.id.record_start_button);
        recordStopButton = findViewById(R.id.record_stop_button);
        playButton = findViewById(R.id.play_button);
        stopButton = findViewById(R.id.stop_button);
        sendStopButton = findViewById(R.id.send_button);
        progressBar = findViewById(R.id.progress);
        textView = findViewById(R.id.text);

        createDirectoriesIfNeeded();

        recordStartButton.setOnClickListener(recordStartClickListener);
        recordStopButton.setOnClickListener(recordStopClickListener);
        sendStopButton.setOnClickListener(sendClickListener);

        playButton.setOnClickListener(playClickListener);
        stopButton.setOnClickListener(stopClickListener);

        wavRecorder = new WavRecorder(audioPlayerName, this);
    }

    private View.OnClickListener recordStartClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            wavRecorder.startRecording();
        }
    };

    private View.OnClickListener recordStopClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            wavRecorder.stopRecording();
        }
    };

    private View.OnClickListener playClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            mediaPlayer = MediaPlayer.create(MainActivity.this, Uri.parse(audioPlayerName));
            mediaPlayer.setOnPreparedListener(preparedListener);
            stopButton.setEnabled(true);

            recordStopButton.setEnabled(false);
            recordStartButton.setEnabled(false);
            playButton.setEnabled(false);
        }
    };

    private View.OnClickListener stopClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            mediaPlayer.stop();

            recordStartButton.setEnabled(true);
            recordStopButton.setEnabled(false);

            playButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    };

    private View.OnClickListener sendClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (Utils.isOnline(getApplicationContext())) {
                uploadFile();
            } else {
                Toast.makeText(MainActivity.this, "No internet Connection!", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void createDirectoriesIfNeeded() {
        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
        File folder = new File(root, "RecordTestForVinay");
        if (!folder.exists()) {
            folder.mkdir();
        }

//        File audioFolder = new File(folder.getAbsolutePath(), "Audio");
//
//        if (!audioFolder.exists()) {
//            audioFolder.mkdir();
//        }

//        root = audioFolder.getAbsolutePath();

        audioPlayerName = folder.getAbsolutePath() + "/" + "audio.wav";
        textView.setText(audioPlayerName);
    }

//    private void setDataSource() {
//        try {
//            if (audioPlayerName != null) {
//                mediaPlayer.setDataSource(audioPlayerName);
//                mediaPlayer.prepare();
//            }
//        } catch (IOException e) {
//            Log.e(TAG, e.getMessage());
//        }
//    }

    private MediaPlayer.OnPreparedListener preparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            mediaPlayer.setOnCompletionListener(completionListener);
            mediaPlayer.start();
        }
    };

    private MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            recordStartButton.setEnabled(true);
            recordStopButton.setEnabled(false);

            playButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    };

    public void getPermissionToRecordAudio() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        RECORD_AUDIO_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRecordStart() {
        recordStopButton.setEnabled(true);

        recordStartButton.setEnabled(false);
        playButton.setEnabled(false);
        stopButton.setEnabled(false);
    }

    @Override
    public void onRecordStop() {
        recordStartButton.setEnabled(true);
        playButton.setEnabled(true);

        recordStopButton.setEnabled(false);
        stopButton.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

    @Override
    public void onRecordError() {

    }

    public void uploadFile() {
        File file = new File(audioPlayerName);

        AndroidNetworking.upload(UPLOAD_URL)
                //TODO if necessary, change the below according to back-end
                .addMultipartFile("image", file)
                //TODO change the below according to back-end
                .addMultipartParameter("key", "value")
                .setTag("uploadTest")
                .setPriority(Priority.HIGH)
                .build()
                .setUploadProgressListener(new UploadProgressListener() {
                    @Override
                    public void onProgress(long bytesUploaded, long totalBytes) {
                        // do anything with progress
                        textView.setText("Bytes Uploaded : " + bytesUploaded);
                        textView.append("\nTotal Bytes : " + totalBytes);
                    }
                })
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        progressBar.setVisibility(View.GONE);
                        // do anything with response
                        textView.setText(response.toString());
                    }

                    @Override
                    public void onError(ANError error) {
                        progressBar.setVisibility(View.GONE);
                        // handle error
                        textView.setText("Error While Uploading : ");
                        textView.append("\n" + error.getErrorBody());
                    }
                });
    }
}
