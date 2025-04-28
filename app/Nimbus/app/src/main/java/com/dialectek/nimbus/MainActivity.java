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
import android.location.Location;

import com.google.android.gms.location.LocationRequest;

import android.os.Bundle;
import android.os.Looper;
import android.os.ParcelUuid;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
   public static String username;
   public static String password;
   public static String id;

   private TextView      mText;
   private ScrollView    mScrollContainer;
   BluetoothLeAdvertiser mAdvertiser;
   ParcelUuid            mUuid;
   AdvertisingSet        mAdvertisingSet;
   private Button        mAdvertiseButton;
   private boolean       mAdvertiseActive;
   BluetoothLeScanner    mBluetoothLeScanner;
   private Button        mDiscoverButton;
   private boolean       mDiscoverActive;
   private Button        mClearTextButton;
   private RadarView mRadarView = null;

   // Discovered IDs.
   static final int                 MAX_ID_AGE_SECS     = 300;
   static final int                 MAX_ID_REFRESH_SECS = 5;
   private TreeMap<String, Instant> mDiscoveredIDs;
   private Instant mDiscoveredIDsAuditTime;

   // Location service.
   private FusedLocationProviderClient mFusedLocationClient;
   private LocationRequest             mLocationRequest;
   private LocationCallback            mLocationCallback;
   private Location mLocation;

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);

      Eula.show(this);

      setContentView(R.layout.activity_main);
      mText               = (TextView)findViewById(R.id.text);
      mScrollContainer    = (ScrollView)findViewById(R.id.scroll_container);
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
      mClearTextButton = (Button)findViewById(R.id.clear_text_btn);
      mClearTextButton.setOnClickListener(this);
      mDiscoveredIDs          = new TreeMap<String, Instant>();
      mDiscoveredIDsAuditTime = Instant.now();

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
            if (mAdvertisingSet != null)
            {
               Double latitude  = mLocation.getLatitude();
               Double longitude = mLocation.getLongitude();
               if ((latitude != null) && (longitude != null))
               {
                  String        serviceData = id + ";" + latitude + "," + longitude;
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
      
      mRadarView = (RadarView) findViewById(R.id.radarView);
      mRadarView.setShowCircles(true);
   }

   public void stopAnimation(View view) {
      if (mRadarView != null) mRadarView.stopAnimation();
   }

   public void startAnimation(View view) {
      if (mRadarView != null) mRadarView.startAnimation();
   }

   @Override
   protected void onStart()
   {
      super.onStart();
      mAdvertiser         = null;
      mBluetoothLeScanner = null;
      requestPermissionsAndEnable();
      mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
      mRadarView.startAnimation();
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
      mRadarView.stopAnimation();
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
         if (grantResults.length >= 4)
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
         String        id      = builder.toString();
         Instant       now     = Instant.now();
         Instant       time    = mDiscoveredIDs.get(id);
         if (time == null)
         {
            mDiscoveredIDs.put(id, now);
            mText.append(new Date() + ": " + id + "\n");
         }
         else
         {
            if (Duration.between(time, now).toSeconds() >= MAX_ID_REFRESH_SECS)
            {
               mDiscoveredIDs.replace(id, now);
               mText.append(new Date() + ": " + id + "\n");
            }
         }
         mScrollContainer.post(new Runnable()
         {
            public void run()
            {
               mScrollContainer.fullScroll(View.FOCUS_DOWN);
            }
         });
         if (Duration.between(mDiscoveredIDsAuditTime, now).toSeconds() >= MAX_ID_AGE_SECS)
         {
            mDiscoveredIDsAuditTime = now;
            TreeMap<String, Instant> tmpIDs = new TreeMap<String, Instant>();
            for (Map.Entry<String, Instant> entry : mDiscoveredIDs.entrySet())
            {
               if (Duration.between(entry.getValue(), now).toSeconds() < MAX_ID_AGE_SECS)
               {
                  tmpIDs.put(entry.getKey(), entry.getValue());
               }
            }
            mDiscoveredIDs = tmpIDs;
         }
      }
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

      String        serviceData = id + ";";
      AdvertiseData data        = new AdvertiseData.Builder()
                                     .addServiceUuid(mUuid)
                                     .addServiceData(mUuid, serviceData.getBytes(Charset.forName("UTF-8")))
                                     .setIncludeDeviceName(false)
                                     .setIncludeTxPowerLevel(false)
                                     .build();

      AdvertiseData scanResponse = new AdvertiseData.Builder()
                                      .addServiceData(mUuid, id.getBytes(Charset.forName("UTF-8")))
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
            startAdvertise();
            mAdvertiseButton.setTextColor(Color.RED);
            mAdvertiseButton.setText("Stop Advertising");
            mAdvertiseActive = true;
         }
      }
      else if (v.getId() == R.id.clear_text_btn)
      {
         mText.setText("");
      }
   }
}
