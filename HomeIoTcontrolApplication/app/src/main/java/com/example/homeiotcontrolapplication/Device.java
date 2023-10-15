package com.example.homeiotcontrolapplication;

import android.content.Intent;
import android.os.Bundle;

import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

public class Device extends AppCompatActivity {
    static boolean on = false;
    static boolean on2 = false;
    static boolean open = false;
    static String pubStateTopic1 = "light/state";
    static String subStateTopic1 = "light/state1";
//    static String pubStateTopic2 = "doorlock/state";
//    static String subStateTopic2 = "doorlock/state1";
    MqttClient client;

    public Device() throws MqttException {
        client = Login.getClient();
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                // Handle MQTT connection loss
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                if (subStateTopic1.equals(topic)) {
                    String jsonStr = new String(message.getPayload());
                    try {
                        JSONObject json = new JSONObject(jsonStr);
                        String light1State = json.getString("light1");
                        String light2State = json.getString("light2");
                        System.out.println("light State : ");
                        System.out.println(light1State);
                        System.out.println(light2State);
                        on = light1State.equals("ON");
                        on2 = light2State.equals("ON");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
//                if (subStateTopic2.equals(topic)) {
//                    String jsonStr = new String(message.getPayload());
//                    try {
//                        JSONObject json = new JSONObject(jsonStr);
//                        String doorlockState = json.getString("doorlock");
//                        System.out.println("doorlock State : ");
//                        System.out.println(doorlockState);
//                        open = doorlockState.equals("ON");
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Handle MQTT message delivery completion
            }
        });
        client.publish(pubStateTopic1, new MqttMessage("Search".getBytes()));
        client.subscribe(subStateTopic1, 0);
//        client.publish(pubStateTopic2, new MqttMessage("Search".getBytes()));
//        client.subscribe(subStateTopic2, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device);

        CardView light_cardview = findViewById(R.id.light_cardview);
        CardView doorlock_cardview = findViewById(R.id.doorlock_cardview);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Toast.makeText(Device.this, "기기 정보를 업데이트 했습니다!", Toast.LENGTH_SHORT).show();

        light_cardview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Device.this, ControlLight.class);
                startActivity(intent);
            }
        });

        doorlock_cardview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Device.this, ControlDoorlock.class);
                startActivity(intent);
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        client = Login.getClient();
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                // Handle MQTT connection loss
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                if (subStateTopic1.equals(topic)) {
                    String jsonStr = new String(message.getPayload());
                    try {
                        JSONObject json = new JSONObject(jsonStr);
                        String light1State = json.getString("light1");
                        String light2State = json.getString("light2");
                        System.out.println("light State : ");
                        System.out.println(light1State);
                        System.out.println(light2State);
                        on = light1State.equals("ON");
                        on2 = light2State.equals("ON");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
//                if (subStateTopic2.equals(topic)) {
//                    String jsonStr = new String(message.getPayload());
//                    try {
//                        JSONObject json = new JSONObject(jsonStr);
//                        String doorlockState = json.getString("doorlock");
//                        System.out.println("doorlock State : ");
//                        System.out.println(doorlockState);
//                        open = doorlockState.equals("ON");
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Handle MQTT message delivery completion
            }
        });
        try {
            client.publish(pubStateTopic1, new MqttMessage("Search".getBytes()));
//            client.publish(pubStateTopic2, new MqttMessage("Search".getBytes()));
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Toast.makeText(Device.this, "기기 정보를 업데이트 했습니다!", Toast.LENGTH_SHORT).show();
            client.subscribe(subStateTopic1, 0);
//            client.subscribe(subStateTopic2, 0);
        }catch (MqttException e){
            e.printStackTrace();
        }

    }
}
