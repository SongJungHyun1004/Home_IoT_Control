package com.example.homeiotcontrolapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

public class ControlDoorlock extends AppCompatActivity {

    static String pubTopic = "homeassistant/doorlock/switch";
    static String subTopic = "homeassistant/doorlock/status";
    boolean open;
    MqttClient client;
    ImageButton doorlock_button;

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

        doorlock_button = findViewById(R.id.doorlock_button);
        if (open)
            doorlock_button.setImageResource(R.drawable.doorlock_open);
        doorlock_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (open) {
                        client.publish(pubTopic, new MqttMessage("LOCK".getBytes()));
                        client.subscribe(subTopic, 0);
                        try {
                            Thread.sleep(4500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        doorlock_button.setImageResource(R.drawable.doorlock_lock);
                    } else {
                        client.publish(pubTopic, new MqttMessage("UNLOCK".getBytes()));
                        client.subscribe(subTopic, 0);
                        try {
                            Thread.sleep(4500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        doorlock_button.setImageResource(R.drawable.doorlock_open);
                    }
                } catch (MqttException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
