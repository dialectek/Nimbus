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
import android.os.Handler;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView mText;
    private Button mAdvertiseButton;
    private Button mDiscoverButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mText = (TextView) findViewById(R.id.text);
        mDiscoverButton = (Button) findViewById(R.id.discover_btn);
        mAdvertiseButton = (Button) findViewById(R.id.advertise_btn);
        mAdvertiseButton.setEnabled(false);
        mDiscoverButton.setEnabled(false);
        mDiscoverButton.setOnClickListener(this);
        mAdvertiseButton.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestPermissions();
        requestEnableBluetooth();
    }

    private void requestPermissions() {
        String[] permissions = new String[]{
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE};
        ActivityCompat.requestPermissions(this, permissions, 2);
    }

    private void requestEnableBluetooth() {
        BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        if (!bluetoothManager.getAdapter().isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode != Activity.RESULT_OK) {
                mAdvertiseButton.setEnabled(false);
                mDiscoverButton.setEnabled(false);
                Toast.makeText(this, "Bluetooth disabled", Toast.LENGTH_SHORT).show();
            }
        }
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
                    mAdvertiseButton.setEnabled(true);
                    mDiscoverButton.setEnabled(true);
                }
            }
        }
    }

    private void discover() {
        BluetoothLeScanner bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();

        List<ScanFilter> filters = new ArrayList<ScanFilter>();
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(UUID.fromString(getString(R.string.ble_uuid))))
                .build();
        filters.add(filter);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                if (result != null) {
                    StringBuilder builder = new StringBuilder(new String(result.getScanRecord().getServiceData(result.getScanRecord().getServiceUuids().get(0)), Charset.forName("UTF-8")));
                    mText.append(new Date() + ": " + builder.toString() + "\n");
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
            }

            @Override
            public void onScanFailed(int errorCode) {
                Toast.makeText(getBaseContext(), "Scan failed: " + errorCode, Toast.LENGTH_SHORT).show();
                //Log.e("BLE", "Discovery onScanFailed: " + errorCode);
                super.onScanFailed(errorCode);
            }
        };

        try {
            bluetoothLeScanner.startScan(filters, settings, scanCallback);
        } catch(Exception e) {
            Toast.makeText(getBaseContext(), "Cannot start scan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void advertise() {
        BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();

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

        AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
            }

            @Override
            public void onStartFailure(int errorCode) {
                Toast.makeText(getBaseContext(), "Advertising failed: " + errorCode, Toast.LENGTH_SHORT).show();
                //Log.e("BLE", "Advertising onStartFailure: " + errorCode);
                super.onStartFailure(errorCode);
            }
        };

        try {
            advertiser.startAdvertising(settings, data, scanResponse, advertisingCallback);
        } catch(Exception e) {
            Toast.makeText(getBaseContext(), "Cannot start advertising: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        if( v.getId() == R.id.discover_btn ) {
            discover();
        } else if( v.getId() == R.id.advertise_btn ) {
            advertise();
        }
    }
}
