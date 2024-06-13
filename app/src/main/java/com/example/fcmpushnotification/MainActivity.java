package com.example.fcmpushnotification;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String SERVER_URL_SINGLE = "http://192.168.18.7:8080/test/send-notification";
    private static final String SERVER_URL_MULTICAST = "http://192.168.18.7:8080/test/multicast-send-notification";

    Button sendBtn, sendAllBtn;
    EditText titleEdt, messageEdt, deviceNameEdt;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views and FusedLocationProviderClient
        titleEdt = findViewById(R.id.title);
        messageEdt = findViewById(R.id.message);
        deviceNameEdt = findViewById(R.id.deviceId);
        sendBtn = findViewById(R.id.send);
        sendAllBtn = findViewById(R.id.sendAll);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        sendAllBtn.setOnClickListener(v -> {
            Log.d(TAG, "Send All button clicked");
            requestLocationPermissions();
        });
    }

    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            getLocationAndSendNotification();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocationAndSendNotification();
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void getLocationAndSendNotification() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            String title = titleEdt.getText().toString();
                            String message = messageEdt.getText().toString();
                            String deviceName = deviceNameEdt.getText().toString();
                            sendNotification(SERVER_URL_MULTICAST, title, message, deviceName, latitude, longitude);
                        } else {
                            Toast.makeText(MainActivity.this, "Unable to get location", Toast.LENGTH_LONG).show();
                        }
                    });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void sendNotification(String url, String title, String message, String deviceName, double latitude, double longitude) {
        Log.d(TAG, "Sending notification to URL: " + url);

        RequestQueue queue = Volley.newRequestQueue(this);

        // Create JSON payload
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("title", title);
            jsonBody.put("body", message);
            jsonBody.put("deviceName", deviceName);
            jsonBody.put("latitude", latitude);
            jsonBody.put("longitude", longitude);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create JSON body", e);
            Toast.makeText(MainActivity.this, "Failed to create JSON body", Toast.LENGTH_LONG).show();
            return;
        }

        final String requestBody = jsonBody.toString();

        // Create a StringRequest with POST method
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Response received: " + response);
                        Toast.makeText(MainActivity.this, "Notification sent: " + response, Toast.LENGTH_LONG).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error sending notification", error);
                        Toast.makeText(MainActivity.this, "Failed to send notification", Toast.LENGTH_LONG).show();
                    }
                }) {
            @Override
            public byte[] getBody() {
                return requestBody == null ? null : requestBody.getBytes();
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };

        // Add the request to the RequestQueue
        queue.add(stringRequest);
    }
}
