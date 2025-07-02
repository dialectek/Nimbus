package com.dialectek.nimbus;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import com.google.android.material.textfield.TextInputEditText;

public class IDactivity extends AppCompatActivity {
   private TextInputEditText id;
   private Button            start;

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_id);
      id       = findViewById(R.id.id);
      start    = findViewById(R.id.start);
      String deviceId = Settings.Secure.getString(
              getApplicationContext().getContentResolver(),
              Settings.Secure.ANDROID_ID);
      if (deviceId == null) {
         Toast.makeText(getBaseContext(), "Cannot get ANDROID_ID", Toast.LENGTH_SHORT).show();
         deviceId = "unknown";
      }
      id.setText(deviceId);
      String finalDeviceId = deviceId;
      start.setOnClickListener(v -> start(finalDeviceId));
   }


   private void start(String id)
   {
      try {
         Intent intent = new Intent(IDactivity.this, MainActivity.class );
         intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
         startActivity(intent);
      }
      catch (Exception e) {
         Toast.makeText(getBaseContext(), "Cannot start main activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
      }
   }
}
