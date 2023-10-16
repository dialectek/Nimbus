// Bluetooth LE advertise/discover.

package com.dialectek.nimbus;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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

    private TextView mText;
    private ScrollView mScrollContainer;
    BluetoothLeAdvertiser mAdvertiser;
    private Button mAdvertiseButton;
    private boolean mAdvertiseActive;
    BluetoothLeScanner mBluetoothLeScanner;
    private Button mDiscoverButton;
    private boolean mDiscoverActive;
    private Button mClearTextButton;

    // Discovered IDs.
    static final int MAX_ID_AGE_SECS = 300;
    static final int MAX_ID_REPORT_SECS = 5;
    private TreeMap<String, Instant> mDiscoveredIDs;
    private Instant mDiscoveredIDsAuditTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Eula.show(this);

        setContentView(R.layout.activity_main);
        mText = (TextView) findViewById(R.id.text);
        mScrollContainer = (ScrollView) findViewById(R.id.scroll_container);
        mBluetoothLeScanner = null;
        mDiscoverActive = false;
        mDiscoverButton = (Button) findViewById(R.id.discover_btn);
        mDiscoverButton.setEnabled(false);
        mDiscoverButton.setOnClickListener(this);
        mAdvertiser = null;
        mAdvertiseActive = false;
        mAdvertiseButton = (Button) findViewById(R.id.advertise_btn);
        mAdvertiseButton.setEnabled(false);
        mAdvertiseButton.setOnClickListener(this);
        mClearTextButton = (Button) findViewById(R.id.clear_text_btn);
        mClearTextButton.setOnClickListener(this);
        mDiscoveredIDs = new TreeMap<String, Instant>();
        mDiscoveredIDsAuditTime = Instant.now();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAdvertiser = null;
        mBluetoothLeScanner = null;
        requestPermissionsAndEnable();
    }

    private void requestPermissionsAndEnable() {
        String[] permissions = new String[]{
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE};
        ActivityCompat.requestPermissions(this, permissions, 2);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2) {
            if (grantResults.length >= 4) {
                boolean locationGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (!locationGranted) {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                }
                boolean scanGranted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                if (!scanGranted) {
                    Toast.makeText(this, "Scan permission denied", Toast.LENGTH_SHORT).show();
                }
                boolean connectGranted = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                if (!connectGranted) {
                    Toast.makeText(this, "Connect permission denied", Toast.LENGTH_SHORT).show();
                }
                boolean advertiseGranted = grantResults[3] == PackageManager.PERMISSION_GRANTED;
                if (!advertiseGranted) {
                    Toast.makeText(this, "Advertise permission denied", Toast.LENGTH_SHORT).show();
                }
                if (!BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported()) {
                    Toast.makeText(this, "Multiple advertisement not supported", Toast.LENGTH_SHORT).show();
                }
                if (locationGranted && scanGranted && connectGranted && advertiseGranted) {
                    requestEnableBluetooth();
                }
            }
        }
    }

    private void requestEnableBluetooth() {
        BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        if (!bluetoothManager.getAdapter().isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        } else {
            mAdvertiseButton.setEnabled(true);
            mDiscoverButton.setEnabled(true);
            mAdvertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
            mBluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                mAdvertiseButton.setEnabled(true);
                mDiscoverButton.setEnabled(true);
                mAdvertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
                mBluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
            } else {
                Toast.makeText(this, "Bluetooth disabled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result != null) {
                StringBuilder builder = new StringBuilder(new String(result.getScanRecord().getServiceData(result.getScanRecord().getServiceUuids().get(0)), Charset.forName("UTF-8")));
                String id = builder.toString();
                Instant now = Instant.now();
                Instant time = mDiscoveredIDs.get(id);
                if (time == null) {
                    mDiscoveredIDs.put(id, now);
                    mText.append(new Date() + ": " + id + "\n");
                } else {
                    if (Duration.between(time, now).toSeconds() >= MAX_ID_REPORT_SECS) {
                        mDiscoveredIDs.replace(id, now);
                        mText.append(new Date() + ": " + id + "\n");
                    }
                }
                if (Duration.between(mDiscoveredIDsAuditTime, now).toSeconds() >= MAX_ID_AGE_SECS) {
                    mDiscoveredIDsAuditTime = now;
                    TreeMap<String, Instant> tmpIDs = new TreeMap<String, Instant>();
                    for (Map.Entry<String, Instant> entry : mDiscoveredIDs.entrySet()) {
                        if (Duration.between(entry.getValue(), now).toSeconds() < MAX_ID_AGE_SECS) {
                            tmpIDs.put(entry.getKey(), entry.getValue());
                        }
                    }
                    mDiscoveredIDs = tmpIDs;
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(getBaseContext(), "Scan failed: " + errorCode, Toast.LENGTH_SHORT).show();
            //Log.e("BLE", "Discovery onScanFailed: " + errorCode);
        }
    };

    private void startDiscover() {
        List<ScanFilter> filters = new ArrayList<ScanFilter>();
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(UUID.fromString(getString(R.string.ble_uuid))))
                .build();
        filters.add(filter);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        try {
            mBluetoothLeScanner.startScan(filters, settings, scanCallback);
        } catch(Exception e) {
            Toast.makeText(getBaseContext(), "Cannot start scan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopDiscover() {
        try {
            mBluetoothLeScanner.stopScan(scanCallback);
        } catch(Exception e) {
            Toast.makeText(getBaseContext(), "Cannot stop scan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Toast.makeText(getBaseContext(), "Advertising failed: " + errorCode, Toast.LENGTH_SHORT).show();
            //Log.e("BLE", "Advertising onStartFailure: " + errorCode);
        }
    };

    private void startAdvertise() {
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .build();

        ParcelUuid pUuid = new ParcelUuid(UUID.fromString(getString(R.string.ble_uuid)));

        AdvertiseData data = new AdvertiseData.Builder()
                .addServiceUuid(pUuid)
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .build();

        String deviceName = BluetoothAdapter.getDefaultAdapter().getName();
        AdvertiseData scanResponse = new AdvertiseData.Builder()
                .addServiceData(pUuid, deviceName.getBytes(Charset.forName("UTF-8")))
                .build();

        try {
            mAdvertiser.startAdvertising(settings, data, scanResponse, advertisingCallback);
        } catch(Exception e) {
            Toast.makeText(getBaseContext(), "Cannot start advertising: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopAdvertise() {
        try {
            mAdvertiser.stopAdvertising(advertisingCallback);
        } catch(Exception e) {
            Toast.makeText(getBaseContext(), "Cannot stop advertising: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        if( v.getId() == R.id.discover_btn ) {
            if (mDiscoverActive) {
                stopDiscover();
                mDiscoverButton.setText("Start Discovering");
                mDiscoverActive = false;
            } else {
                startDiscover();
                mDiscoverButton.setText("Stop Discovering");
                mDiscoverActive = true;
            }
        } else if( v.getId() == R.id.advertise_btn ) {
            if (mAdvertiseActive) {
                stopAdvertise();
                mAdvertiseButton.setText("Start Advertising");
                mAdvertiseActive = false;
            } else {
                startAdvertise();
                mAdvertiseButton.setText("Stop Advertising");
                mAdvertiseActive = true;
            }
        } else if( v.getId() == R.id.clear_text_btn ) {
            mText.setText("");
        }
    }
}
