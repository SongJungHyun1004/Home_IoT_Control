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

public class ControlLight extends AppCompatActivity {

    static String pubTopic1 = "homeassistant/light/light1/switch";
    static String pubTopic2 = "homeassistant/light/light2/switch";
    static String subTopic = "homeassistant/light/status";
    boolean on;
    boolean on2;
    MqttClient client;
    ImageButton light_button1;
    ImageButton light_button2;

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

        light_button1 = findViewById(R.id.light_button1);
        if (on)
            light_button1.setImageResource(R.drawable.light_on_button);
        light_button2 = findViewById(R.id.light_button2);
        if (on2)
            light_button2.setImageResource(R.drawable.light_on_button);

        light_button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (on) {
                        client.publish(pubTopic1, new MqttMessage("OFF".getBytes()));
                        client.subscribe(subTopic, 0);
                        try {
                            Thread.sleep(4500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        light_button1.setImageResource(R.drawable.light_off_button);
                    } else {
                        client.publish(pubTopic1, new MqttMessage("ON".getBytes()));
                        client.subscribe(subTopic, 0);
                        try {
                            Thread.sleep(4500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        light_button1.setImageResource(R.drawable.light_on_button);
                    }
                } catch (MqttException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        light_button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (on2) {
                        client.publish(pubTopic2, new MqttMessage("OFF".getBytes()));
                        client.subscribe(subTopic, 0);
                        try {
                            Thread.sleep(4500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        light_button2.setImageResource(R.drawable.light_off_button);
                    } else {
                        client.publish(pubTopic2, new MqttMessage("ON".getBytes()));
                        client.subscribe(subTopic, 0);
                        try {
                            Thread.sleep(4500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        light_button2.setImageResource(R.drawable.light_on_button);
                    }
                } catch (MqttException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

}
