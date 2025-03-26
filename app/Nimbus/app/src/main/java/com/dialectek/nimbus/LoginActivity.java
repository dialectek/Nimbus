package com.dialectek.nimbus;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText username;
    private TextInputEditText password;
    private TextInputEditText id;
    private Button login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        id = findViewById(R.id.id);
        login = findViewById(R.id.login);
        login.setOnClickListener(v -> login(username.getText().toString(),
                password.getText().toString(), id.getText().toString()));
    }

    private void login(String username, String password, String id) {
        MainActivity.username = username;
        MainActivity.password = password;
        MainActivity.id = id;
        try {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), "Cannot start main activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}