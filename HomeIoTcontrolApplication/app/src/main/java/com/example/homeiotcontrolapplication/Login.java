package com.example.homeiotcontrolapplication;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class Login extends AppCompatActivity {
    private static MqttClient client;
    static String MQTTHOST = "ssl://a287snqybyvrg6-ats.iot.ap-northeast-2.amazonaws.com:8883";

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
                AssetManager assetManager = getAssets();
                try {
                    MqttConnectOptions option = new MqttConnectOptions();
                    if (!password.equals("password")){
                        throw new Exception();
                    }
                    String certFileName = "614009cb03aac1441d1fb3ee3aba5f3dc1ffe8ff626e0b1f610f1e7289337929-certificate.pem.crt";
                    String privateKeyFileName = "614009cb03aac1441d1fb3ee3aba5f3dc1ffe8ff626e0b1f610f1e7289337929-private.pem.key";
                    String caFileName = "AmazonRootCA1.pem";
                    // AssetManager를 사용하여 파일을 읽음
                    InputStream certStream = assetManager.open(certFileName);
                    InputStream caStream = assetManager.open(caFileName);
                    InputStream privateKeyStream = assetManager.open(privateKeyFileName);

                    // SSL 소켓 팩토리 생성
                    SSLSocketFactory socketFactory = createSSLSocketFactory(certStream, privateKeyStream, caStream);
                    option.setSocketFactory(socketFactory);
                    client = new MqttClient(MQTTHOST, username, null);
                    System.out.println("connecting start!!!!");
                    client.connect(option);
                    Toast.makeText(Login.this, "MQTT connection success!", Toast.LENGTH_SHORT).show();
                    System.out.println("connecting success!!!!");
                    Intent intent = new Intent(getApplicationContext(), Device.class);
                    startActivity(intent);
                }catch (MqttException e){
                    e.printStackTrace();
                    Toast.makeText(Login.this, "MQTT connection failed!", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(Login.this, "Failed", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(Login.this, "Login error", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public SSLSocketFactory createSSLSocketFactory(InputStream certStream, InputStream privateKeyStream, InputStream caStream) throws Exception {
        try {
            // Load the Android Keystore
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            // CertificateFactory를 사용하여 X.509 형식의 인증서와 CA를 로드
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate caCert = (X509Certificate) certificateFactory.generateCertificate(caStream);
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(certStream);

            // 개인 키 파일에서 개인 키를 로드
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(readPemFile(privateKeyStream));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            // KeyPair 생성
            KeyPair keyPair = new KeyPair(certificate.getPublicKey(), privateKey);

            // Android Keystore에 인증서와 개인 키 저장
            keyStore.setCertificateEntry("ca", caCert);
            keyStore.setKeyEntry("certificate", keyPair.getPrivate(), null, new Certificate[]{certificate});

            // SSL 소켓 팩토리 생성
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, null);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

            return sslContext.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 개인 키 파일을 읽어 byte 배열로 반환
    private byte[] readPemFile(InputStream inputStream) {
        try {
            StringBuilder pemData = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("-----BEGIN") && !line.startsWith("-----END")) {
                    pemData.append(line);
                }
            }
            reader.close();

            return Base64.decode(pemData.toString(), Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static MqttClient getClient() {
        return client;
    }
}
