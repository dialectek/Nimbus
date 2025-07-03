// Bluetooth LE advertise/discover.

package com.dialectek.nimbus;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;

import com.google.android.gms.location.LocationRequest;

import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, SensorEventListener {

   // UI.
   private String mName = null;
   private String mID = null;
   private TextView mNameText;
   private TextView      mDiscoveredText;
   private ScrollView    mDiscoveredScroll;
   private Button        mAdvertiseButton;
   private boolean       mAdvertiseActive;
   private Button        mDiscoverButton;
   private boolean       mDiscoverActive;
   private Button mClearDiscoveredButton;
   private RadarView mRadarView;
   public static Slider RangeSlider;
   private Random mRandom;

   // Discovered IDs.
   static final int                 MAX_ID_AGE_SECS     = 10;
   static final int                 MAX_ID_REFRESH_MS = 500;
   public static TreeMap<String, ID> DiscoveredIDs;
   android.os.Handler mHandler;
   Runnable           mTick;

   // Bluetooth.
   BluetoothLeAdvertiser mAdvertiser;
   ParcelUuid            mUuid;
   AdvertisingSet        mAdvertisingSet;
   BluetoothLeScanner    mBluetoothLeScanner;

   // Location service.
   private FusedLocationProviderClient mFusedLocationClient;
   private LocationRequest             mLocationRequest;
   private LocationCallback            mLocationCallback;
   private Location mLocation;

   // Compass.
   private SensorManager sensorManager;
   private Sensor accelerometer;
   private Sensor magnetometer;
   private float[] accelerometerData = new float[3];
   private float[] magnetometerData = new float[3];
   public static boolean isAccelerometerSet = false;
   public static boolean isMagnetometerSet = false;
   public static float CompassBearing = 0.0f;

   // Server.
   private final String SERVER_ADDRESS = "ec2-3-21-97-64.us-east-2.compute.amazonaws.com";
   private WebSocketClient mClient;

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);

      Eula.show(this);

      setContentView(R.layout.activity_main);
      mNameText       = findViewById(R.id.name_text);
      mID = Settings.Secure.getString(
              getApplicationContext().getContentResolver(),
              Settings.Secure.ANDROID_ID);
      if (mID == null) {
         Toast.makeText(getBaseContext(), "Cannot get ANDROID_ID", Toast.LENGTH_SHORT).show();
      }
      connectServer();
      mDiscoveredText               = (TextView)findViewById(R.id.discovered_text);
      mDiscoveredText.setMovementMethod(LinkMovementMethod.getInstance());
      mDiscoveredScroll    = (ScrollView)findViewById(R.id.discovered_scroll);
      mBluetoothLeScanner = null;
      mDiscoverActive     = false;
      mDiscoverButton     = (Button)findViewById(R.id.discover_btn);
      mDiscoverButton.setEnabled(false);
      mDiscoverButton.setOnClickListener(this);
      mAdvertiser      = null;
      mUuid            = null;
      mAdvertisingSet  = null;
      mAdvertiseActive = false;
      mAdvertiseButton = (Button)findViewById(R.id.advertise_btn);
      mAdvertiseButton.setEnabled(false);
      mAdvertiseButton.setOnClickListener(this);
      mClearDiscoveredButton = (Button)findViewById(R.id.clear_discovered_btn);
      mClearDiscoveredButton.setOnClickListener(this);
      mRandom = new Random();
      DiscoveredIDs          = new TreeMap<String, ID>();
      mHandler = new android.os.Handler();
      mTick    = new Runnable()
      {
         @Override
         public void run()
         {
            refreshIDs(Instant.now());
            mHandler.postDelayed(this, MAX_ID_REFRESH_MS);
         }
      };

      // Initialize location service.
      mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
      mLocationRequest     = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                                .setMinUpdateIntervalMillis(5000)
                                .setMaxUpdateDelayMillis(15000)
                                .setWaitForAccurateLocation(false)
                                .build();
      mLocationCallback = new LocationCallback()
      {
         @Override
         public void onLocationResult(LocationResult locationResult)
         {
            super.onLocationResult(locationResult);
            mLocation = locationResult.getLastLocation();
            if (mName != null && mAdvertisingSet != null)
            {
               Double latitude  = mLocation.getLatitude();
               Double longitude = mLocation.getLongitude();
               if ((latitude != null) && (longitude != null))
               {
                  String        serviceData = mName + ";" + latitude + "," + longitude;
                  AdvertiseData data        = new AdvertiseData.Builder()
                                                 .addServiceUuid(mUuid)
                                                 .addServiceData(mUuid, serviceData.getBytes(Charset.forName("UTF-8")))
                                                 .setIncludeDeviceName(false)
                                                 .setIncludeTxPowerLevel(false)
                                                 .build();
                  mAdvertisingSet.setAdvertisingData(data);
               }
            }
         }
      };
      mLocation = null;

      // Compass.
      sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
      accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
      magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

      // Radar.
      mRadarView = (RadarView) findViewById(R.id.radarView);

      // Range slider.
      RangeSlider = findViewById(R.id.range_slider);
      RangeSlider.setLabelFormatter(new LabelFormatter() {
         @NonNull
         @Override
         public String getFormattedValue(float value) {
            return (int)value + "m";
         }
      });
   }

   @Override
   protected void onStart()
   {
      super.onStart();
      mAdvertiser         = null;
      mBluetoothLeScanner = null;
      requestPermissionsAndEnable();
      mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
      sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
      sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
      mHandler.removeCallbacks(mTick);
      mHandler.post(mTick);
      mRadarView.startUpdate();
   }

   @Override
   protected void onStop()
   {
      super.onStop();
      stopDiscover();
      mDiscoverButton.setTextColor(Color.BLACK);
      mDiscoverButton.setText("Start Discovering");
      mDiscoverActive = false;
      stopAdvertise();
      mAdvertiseButton.setTextColor(Color.BLACK);
      mAdvertiseButton.setText("Start Advertising");
      mAdvertiseActive = false;
      mFusedLocationClient.removeLocationUpdates(mLocationCallback);
      sensorManager.unregisterListener(this);
      mHandler.removeCallbacks(mTick);
      mRadarView.stopUpdate();
   }

   private void requestPermissionsAndEnable()
   {
      String[] permissions = new String[] {
         android.Manifest.permission.ACCESS_FINE_LOCATION,
         android.Manifest.permission.BLUETOOTH_SCAN,
         Manifest.permission.BLUETOOTH_CONNECT,
         Manifest.permission.BLUETOOTH_ADVERTISE,
         android.Manifest.permission.INTERNET
      };
      ActivityCompat.requestPermissions(this, permissions, 2);
   }


   @Override
   public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
   {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
      if (requestCode == 2)
      {
         if (grantResults.length >= 5)
         {
            boolean locationGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (!locationGranted)
            {
               Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
            boolean scanGranted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
            if (!scanGranted)
            {
               Toast.makeText(this, "Scan permission denied", Toast.LENGTH_SHORT).show();
            }
            boolean connectGranted = grantResults[2] == PackageManager.PERMISSION_GRANTED;
            if (!connectGranted)
            {
               Toast.makeText(this, "Connect permission denied", Toast.LENGTH_SHORT).show();
            }
            boolean advertiseGranted = grantResults[3] == PackageManager.PERMISSION_GRANTED;
            if (!advertiseGranted)
            {
               Toast.makeText(this, "Advertise permission denied", Toast.LENGTH_SHORT).show();
            }
            boolean internetGranted = grantResults[4] == PackageManager.PERMISSION_GRANTED;
            if (!internetGranted)
            {
               Toast.makeText(this, "Internet permission denied", Toast.LENGTH_SHORT).show();
            }
            if (!BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported())
            {
               Toast.makeText(this, "Multiple advertisement not supported", Toast.LENGTH_SHORT).show();
            }
            if (locationGranted && scanGranted && connectGranted && advertiseGranted)
            {
               requestEnableBluetooth();
            }
         }
      }
   }

   private void requestEnableBluetooth()
   {
      BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);

      if (!bluetoothManager.getAdapter().isEnabled())
      {
         Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
         startActivityForResult(enableBtIntent, 1);
      }
      else
      {
         mAdvertiseButton.setEnabled(true);
         mDiscoverButton.setEnabled(true);
         mAdvertiser         = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
         mBluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
      }
   }

   @Override
   public void onActivityResult(int requestCode, int resultCode, Intent data)
   {
      super.onActivityResult(requestCode, resultCode, data);
      if (requestCode == 1)
      {
         if (resultCode == Activity.RESULT_OK)
         {
            mAdvertiseButton.setEnabled(true);
            mDiscoverButton.setEnabled(true);
            mAdvertiser         = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
            mBluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
         }
         else
         {
            Toast.makeText(this, "Bluetooth disabled", Toast.LENGTH_SHORT).show();
         }
      }
   }

   ScanCallback scanCallback = new ScanCallback()
   {
      @Override
      public void onScanResult(int callbackType, ScanResult result)
      {
         super.onScanResult(callbackType, result);
         registerID(result);
      }


      @Override
      public void onBatchScanResults(List<ScanResult> results)
      {
         super.onBatchScanResults(results);
         for (ScanResult result : results)
         {
            registerID(result);
         }
      }


      @Override
      public void onScanFailed(int errorCode)
      {
         super.onScanFailed(errorCode);
         Toast.makeText(getBaseContext(), "Scan failed: " + errorCode, Toast.LENGTH_SHORT).show();
      }
   };

   // Register id.
   private void registerID(ScanResult result)
   {
      if (result != null)
      {
         StringBuilder builder = new StringBuilder(new String(result.getScanRecord().getServiceData(result.getScanRecord().getServiceUuids().get(0)), Charset.forName("UTF-8")));
         String        name      = builder.toString();
         double latitude = 0.0;
         double longitude = 0.0;
         float distance = -1.0f;
         float xDist = -1.0f;
         float yDist = -1.0f;
         if (name.contains(";")) {
            String[] nameLatLong = name.split(";");
            if (nameLatLong != null && nameLatLong.length > 0) {
               name = nameLatLong[0];
               if (nameLatLong.length >= 2 && mLocation != null) {
                  String[] latLong = nameLatLong[1].split(",");
                  if (latLong != null && latLong.length >= 2) {
                     latitude = Double.parseDouble(latLong[0]);
                     longitude = Double.parseDouble(latLong[1]);
                     double myLatitude = mLocation.getLatitude();
                     double myLongitude = mLocation.getLongitude();
                     float[] results = new float[1];
                     Location.distanceBetween(myLatitude, myLongitude, latitude, longitude, results);
                     distance = results[0];
                     Location.distanceBetween(myLatitude, myLongitude, myLatitude, longitude, results);
                     xDist = results[0];
                     if (myLongitude > longitude) {
                        xDist = -xDist;
                     }
                     Location.distanceBetween(myLatitude, myLongitude, latitude, myLongitude, results);
                     yDist = results[0];
                     if (myLatitude > latitude) {
                        yDist = -yDist;
                     }
                  }
               }
            }
         }
         Instant       now     = Instant.now();
         ID data    = DiscoveredIDs.get(name);
         if (data == null)
         {
            mRandom.setSeed(name.hashCode());
            int red = mRandom.nextInt(256);
            int green = mRandom.nextInt(256);
            int blue = mRandom.nextInt(256);
            int color = Color.argb(255, red, green, blue);
            data = new ID(name, color, distance, xDist, yDist, latitude, longitude, now);
         }
         else
         {
            data.distance = distance;
            data.xDist = xDist;
            data.yDist = yDist;
            data.latitude = latitude;
            data.longitude = longitude;
            data.time = now;
         }
         DiscoveredIDs.put(name, data);
         refreshIDs(now);
      }
   }

   // Refresh IDs.
   private synchronized void refreshIDs(Instant now) {
      mDiscoveredText.setText("");
      TreeMap<String, ID> tmpIDs = new TreeMap<String, ID>();
      for (Map.Entry<String, ID> entry : DiscoveredIDs.entrySet())
      {
         String id = entry.getKey();
         ID data = entry.getValue();
         if (Duration.between(data.time, now).toSeconds() < MAX_ID_AGE_SECS)
         {
            tmpIDs.put(id, data);
            appendText(mDiscoveredText, id,  ";lat=" + data.latitude + ",long=" + data.longitude + ";dist=" + data.distance +
                    ",dx=" + data.xDist + ",dy=" + data.yDist + "\n", data.color);
         }
      }
      DiscoveredIDs = tmpIDs;
   }

   // Append formatted text.
   public void appendText(TextView tv, String id, String text, int color) {
      int start = tv.getText().length();
      ClickableSpan clickableSpan = new ClickableSpan() {
         public void onClick(View widget) {
            Intent link = new Intent( Intent.ACTION_VIEW ,
                    Uri.parse("http://dialectek.com/Nimbus/" + id + ".html"));
            startActivity(link);
         }
      };
      tv.append(id);
      int end = tv.getText().length();
      Spannable spannableText = (Spannable) tv.getText();
      spannableText.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      tv.append(text);
      end = tv.getText().length();
      spannableText = (Spannable) tv.getText();
      spannableText.setSpan(new ForegroundColorSpan(color), start, end, 0);
   }

   private void startDiscover()
   {
      List<ScanFilter> filters = new ArrayList<ScanFilter>();
      ScanFilter       filter  = new ScanFilter.Builder()
                                    .setServiceUuid(new ParcelUuid(UUID.fromString(getString(R.string.ble_uuid))))
                                    .build();
      filters.add(filter);

      ScanSettings settings = new ScanSettings.Builder()
                                 .setLegacy(false)
                                 .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                                 .build();

      try {
         mBluetoothLeScanner.startScan(filters, settings, scanCallback);
      }
      catch (Exception e) {
         Toast.makeText(getBaseContext(), "Cannot start scan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
      }
   }


   private void stopDiscover()
   {
      try {
         mBluetoothLeScanner.stopScan(scanCallback);
      }
      catch (Exception e) {
         Toast.makeText(getBaseContext(), "Cannot stop scan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
      }
   }


   AdvertiseCallback advertisingCallback = new AdvertiseCallback()
   {
      @Override
      public void onStartSuccess(AdvertiseSettings settingsInEffect)
      {
         super.onStartSuccess(settingsInEffect);
      }


      @Override
      public void onStartFailure(int errorCode)
      {
         super.onStartFailure(errorCode);
         Toast.makeText(getBaseContext(), "Advertising failed: " + errorCode, Toast.LENGTH_SHORT).show();
      }
   };

   AdvertisingSetCallback advertisingSetCallback = new AdvertisingSetCallback()
   {
      @Override
      public void onAdvertisingSetStarted(AdvertisingSet advertisingSet, int txPower, int status)
      {
         mAdvertisingSet = advertisingSet;
      }


      @Override
      public void onAdvertisingDataSet(AdvertisingSet advertisingSet, int status)
      {
      }


      @Override
      public void onScanResponseDataSet(AdvertisingSet advertisingSet, int status)
      {
      }


      @Override
      public void onAdvertisingSetStopped(AdvertisingSet advertisingSet)
      {
      }
   };

   private void startAdvertise()
   {
      if (mName == null) {
         Toast.makeText(getBaseContext(), "Cannot start advertising: name unavailable", Toast.LENGTH_SHORT).show();
         return;
      }

      // Check if features are supported
      BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

      if (!adapter.isLe2MPhySupported())
      {
         Toast.makeText(getBaseContext(), "2M PHY not supported", Toast.LENGTH_SHORT).show();
         return;
      }
      if (!adapter.isLeExtendedAdvertisingSupported())
      {
         Toast.makeText(getBaseContext(), "LE Extended Advertising not supported", Toast.LENGTH_SHORT).show();
         return;
      }

      AdvertisingSetParameters parameters = (new AdvertisingSetParameters.Builder())
                                               .setLegacyMode(false)
                                               .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
                                               .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
                                               .setConnectable(false)
                                               .setPrimaryPhy(BluetoothDevice.PHY_LE_1M)
                                               .setSecondaryPhy(BluetoothDevice.PHY_LE_2M)
                                               .build();

      mUuid = new ParcelUuid(UUID.fromString(getString(R.string.ble_uuid)));

      String        serviceData = mName + ";";
      AdvertiseData data        = new AdvertiseData.Builder()
                                     .addServiceUuid(mUuid)
                                     .addServiceData(mUuid, serviceData.getBytes(Charset.forName("UTF-8")))
                                     .setIncludeDeviceName(false)
                                     .setIncludeTxPowerLevel(false)
                                     .build();

      AdvertiseData scanResponse = new AdvertiseData.Builder()
                                      .addServiceData(mUuid, mName.getBytes(Charset.forName("UTF-8")))
                                      .build();

      try {
         mAdvertiser.startAdvertisingSet(parameters, data, scanResponse, null, null, advertisingSetCallback);
      }
      catch (Exception e) {
         Toast.makeText(getBaseContext(), "Cannot start advertising: " + e.getMessage(), Toast.LENGTH_SHORT).show();
      }
   }


   private void stopAdvertise()
   {
      try {
         mAdvertiser.stopAdvertisingSet(advertisingSetCallback);
      }
      catch (Exception e) {
         Toast.makeText(getBaseContext(), "Cannot stop advertising: " + e.getMessage(), Toast.LENGTH_SHORT).show();
      }
   }

   @Override
   public void onClick(View v)
   {
      if (v.getId() == R.id.discover_btn)
      {
         if (mDiscoverActive)
         {
            stopDiscover();
            mDiscoverButton.setTextColor(Color.BLACK);
            mDiscoverButton.setText("Start Discovering");
            mDiscoverActive = false;
         }
         else
         {
            startDiscover();
            mDiscoverButton.setTextColor(Color.RED);
            mDiscoverButton.setText("Stop Discovering");
            mDiscoverActive = true;
         }
      }
      else if (v.getId() == R.id.advertise_btn)
      {
         if (mAdvertiseActive)
         {
            stopAdvertise();
            mAdvertiseButton.setTextColor(Color.BLACK);
            mAdvertiseButton.setText("Start Advertising");
            mAdvertiseActive = false;
         }
         else
         {
            if (mName == null) {
               Toast.makeText(getBaseContext(), "Cannot start advertising: name unavailable", Toast.LENGTH_SHORT).show();
            } else {
               startAdvertise();
               mAdvertiseButton.setTextColor(Color.RED);
               mAdvertiseButton.setText("Stop Advertising");
               mAdvertiseActive = true;
            }
         }
      }
      else if (v.getId() == R.id.clear_discovered_btn)
      {
         DiscoveredIDs.clear();
         mDiscoveredText.setText("");
      }
   }

   @Override
   public void onSensorChanged(SensorEvent sensorEvent) {

      int sensorType = sensorEvent.sensor.getType();
      switch (sensorType) {
         case Sensor.TYPE_ACCELEROMETER:
            accelerometerData = sensorEvent.values.clone();
            isAccelerometerSet = true;
            break;
         case Sensor.TYPE_MAGNETIC_FIELD:
            magnetometerData = sensorEvent.values.clone();
            isMagnetometerSet = true;
            break;
         default:
            return;
      }
      if (!isAccelerometerSet || !isMagnetometerSet) {
         return;
      }

      float[] rotationMatrix = new float[9];
      boolean rotationOK = SensorManager.getRotationMatrix(rotationMatrix,
              null, accelerometerData, magnetometerData);

      float orientationValues[] = new float[3];
      if (rotationOK) {
         SensorManager.getOrientation(rotationMatrix,
                 orientationValues);
         float azimuth = orientationValues[0];
         CompassBearing = -(float) Math.toDegrees(azimuth);
      }
   }

   @Override
   public void onAccuracyChanged(Sensor sensor, int accuracy) {}

   // Server communications.
   private void connectServer() {
      URI uri;
      try {
         uri = new URI("ws://" + SERVER_ADDRESS + ":8025/ws/server");
      } catch (URISyntaxException e) {
         Toast.makeText(this, "Invalid server URI", Toast.LENGTH_SHORT).show();
         return;
      }

      mClient = new WebSocketClient(uri) {
         @Override
         public void onOpen(ServerHandshake serverHandshake) {
            if (mID != null) {
               send("id_to_name:" + mID);
            }
            toastMessage("Connected to server");
         }

         @Override
         public void onMessage(String message) {
            if (message.startsWith("id_to_name:")) {
               mName = message.split(":")[1];
               mNameText.setText(mName);
            }
            toastMessage("Message from server: " + message);
         }

         @Override
         public void onClose(int i, String s, boolean b) {
            toastMessage("Server connection closed: " + s);
         }

         @Override
         public void onError(Exception e) {
            toastMessage("Server connection error: " + e.getMessage());
         }

         private void toastMessage(String message) {
            runOnUiThread(new Runnable() {
               @Override
               public void run() {
                  Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
               }
            });
         }
      };
      mClient.connect();
   }
}
