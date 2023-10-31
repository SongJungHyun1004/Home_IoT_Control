package com.example.homeiotcontrolapplication;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ControlDoorlock extends AppCompatActivity {

    static String pubTopic = "homeassistant/doorlock/switch";
    static String subTopic = "homeassistant/doorlock/status";
    boolean open;
    MqttClient client;
    ImageButton doorlock_button;
    TextView textView;
    ImageButton stt_button;
    final int PERMISSION = 1;

    public ControlDoorlock() {
        open = Device.open;
        client = Login.getClient();
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                // Handle MQTT connection loss
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                if (subTopic.equals(topic)) {
                    String jsonStr = new String(message.getPayload());
                    try {
                        JSONObject json = new JSONObject(jsonStr);
                        String doorlockState = json.getString("doorlock");
                        System.out.print("doorlock State : ");
                        System.out.println(doorlockState);
                        open = doorlockState.equals("UNLOCK");
                        if (open)
                            doorlock_button.setImageResource(R.drawable.doorlock_open);
                        else
                            doorlock_button.setImageResource(R.drawable.doorlock_lock);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Handle MQTT message delivery completion
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.control_doorlock);

        if (Build.VERSION.SDK_INT >= 23){
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.INTERNET, Manifest.permission.RECORD_AUDIO}, PERMISSION);
        }

        textView = findViewById(R.id.stt_result);
        stt_button = findViewById(R.id.stt_button);

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR");

        stt_button.setOnClickListener(v -> {
            SpeechRecognizer mRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            mRecognizer.setRecognitionListener(listener);
            mRecognizer.startListening(intent);
        });

        doorlock_button = findViewById(R.id.doorlock_button);
        if (open)
            doorlock_button.setImageResource(R.drawable.doorlock_open);
        doorlock_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (open) {
                    closeDoor();
                } else {
                    openDoor();
                }
            }
        });
    }

    private void openDoor() {
        try {
            client.publish(pubTopic, new MqttMessage("UNLOCK".getBytes()));
            client.subscribe(subTopic, 0);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            doorlock_button.setImageResource(R.drawable.doorlock_open);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    private void closeDoor() {
        try {
            client.publish(pubTopic, new MqttMessage("LOCK".getBytes()));
            client.subscribe(subTopic, 0);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            doorlock_button.setImageResource(R.drawable.doorlock_lock);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    private RecognitionListener listener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle bundle) {
            Toast.makeText(getApplicationContext(),"음성인식을 시작합니다.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onBeginningOfSpeech() {}

        @Override
        public void onRmsChanged(float rmsdB) {}

        @Override
        public void onBufferReceived(byte[] buffer) {}

        @Override
        public void onEndOfSpeech() {}

        @Override
        public void onError(int error) {
            String message;

            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    message = "오디오 에러";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    message = "클라이언트 에러";
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    message = "퍼미션 없음";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    message = "네트워크 에러";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    message = "네트웍 타임아웃";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    message = "찾을 수 없음";
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    message = "RECOGNIZER가 바쁨";
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    message = "서버가 이상함";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    message = "말하는 시간초과";
                    break;
                default:
                    message = "알 수 없는 오류임";
                    break;
            }

            Toast.makeText(getApplicationContext(), "에러가 발생하였습니다. : " + message,Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            for(int i = 0; i < matches.size() ; i++){
                textView.setText(matches.get(i));
            }
            if (textView.getText().equals("문 열어")) {
                System.out.println("문 열기 호출");
                openDoor();
            }
            if (textView.getText().equals("문 닫아")) {
                System.out.println("문 닫기 호출");
                closeDoor();
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {}

        @Override
        public void onEvent(int eventType, Bundle params) {}
    };
}
