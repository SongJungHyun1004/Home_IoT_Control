package com.example.homeiotcontrolapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

public class Login extends AppCompatActivity {
    private static MqttClient client;
    static String MQTTHOST = "tcp://192.168.243.198:1883";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        Button login_button = findViewById(R.id.login_button);
        login_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText enterId = findViewById(R.id.enter_id);
                EditText enterPassword = findViewById(R.id.enter_passward);
                String username = enterId.getText().toString();
                String password = enterPassword.getText().toString();
                System.out.println(username);
                System.out.println(password);
                try {
                    MqttConnectOptions option = new MqttConnectOptions();
                    option.setUserName(username);
                    option.setPassword(password.toCharArray());
                    client = new MqttClient(MQTTHOST, MqttClient.generateClientId(), null);
                    System.out.println("connecting start!!!!");
                    client.connect(option);
                    Toast.makeText(Login.this, "MQTT connection success!", Toast.LENGTH_SHORT).show();
                    System.out.println("connecting success!!!!");
                    Intent intent = new Intent(getApplicationContext(), Device.class);
                    startActivity(intent);
                }catch (MqttException e){
                    e.printStackTrace();
                    Toast.makeText(Login.this, "MQTT connection failed!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    public static MqttClient getClient() {
        return client;
    }
}
