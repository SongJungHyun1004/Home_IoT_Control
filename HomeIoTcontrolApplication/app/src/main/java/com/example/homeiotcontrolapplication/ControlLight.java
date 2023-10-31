package com.example.homeiotcontrolapplication;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
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

public class ControlLight extends AppCompatActivity {

    static String pubTopic1 = "homeassistant/light/light1/switch";
    static String pubTopic2 = "homeassistant/light/light2/switch";
    static String subTopic = "homeassistant/light/status";
    boolean on;
    boolean on2;
    MqttClient client;
    ImageButton light_button1;
    ImageButton light_button2;
    TextView textView;
    ImageButton stt_button;
    final int PERMISSION = 1;

    public ControlLight() {
        on = Device.on;
        on2 = Device.on2;
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
                        String light1State = json.getString("light1");
                        String light2State = json.getString("light2");
                        System.out.print("light1State : ");
                        System.out.println(light1State);
                        System.out.println(light2State);
                        on = light1State.equals("ON");
                        on2 = light2State.equals("ON");
                        if (on)
                            light_button1.setImageResource(R.drawable.light_on_button);
                        else
                            light_button1.setImageResource(R.drawable.light_off_button);
                        if (on2)
                            light_button2.setImageResource(R.drawable.light_on_button);
                        else
                            light_button2.setImageResource(R.drawable.light_off_button);
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
        setContentView(R.layout.control_light);

        if (Build.VERSION.SDK_INT >= 23){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET, Manifest.permission.RECORD_AUDIO}, PERMISSION);
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

        light_button1 = findViewById(R.id.light_button1);
        if (on)
            light_button1.setImageResource(R.drawable.light_on_button);
        light_button2 = findViewById(R.id.light_button2);
        if (on2)
            light_button2.setImageResource(R.drawable.light_on_button);

        light_button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (on) {
                    turnOff(light_button1, pubTopic1);
                } else {
                    turnOn(light_button1, pubTopic1);
                }
            }
        });

        light_button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (on2) {
                    turnOff(light_button2, pubTopic2);
                } else {
                    turnOn(light_button2, pubTopic2);
                }
            }
        });
    }

    private void turnOn(ImageButton light_button, String pubTopic) {
        try {
            client.publish(pubTopic, new MqttMessage("ON".getBytes()));
            client.subscribe(subTopic, 0);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            light_button.setImageResource(R.drawable.light_on_button);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    private void turnOff(ImageButton light_button, String pubTopic) {
        try {
            client.publish(pubTopic, new MqttMessage("OFF".getBytes()));
            client.subscribe(subTopic, 0);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            light_button.setImageResource(R.drawable.light_off_button);
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
            if (textView.getText().equals("1번 불 켜 줘")) {
                System.out.println("불 켜기 호출");
                turnOn(light_button1, pubTopic1);
            }
            if (textView.getText().equals("1번 불 꺼 줘")) {
                System.out.println("불 끄기 호출");
                turnOff(light_button1, pubTopic1);
            }
            if (textView.getText().equals("2번 불 켜 줘") || textView.getText().equals("이번 불 켜 줘")) {
                System.out.println("불 켜기 호출");
                turnOn(light_button2, pubTopic2);
            }
            if (textView.getText().equals("2번 불 꺼 줘") || textView.getText().equals("이번 불 꺼 줘")) {
                System.out.println("불 끄기 호출");
                turnOff(light_button2, pubTopic2);
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {}

        @Override
        public void onEvent(int eventType, Bundle params) {}
    };
}
