package com.dialectek.nimbus;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CAMERA;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView mText;
    private Button mAdvertiseButton;
    private Button mDiscoverButton;

    private BluetoothLeScanner mBluetoothLeScanner;
    private Handler mHandler = new Handler();

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

        mBluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();

        String[] permissions = new String[] {
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE };
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
                    mAdvertiseButton.setEnabled(true);
                    mDiscoverButton.setEnabled(true);
                }
            }
        }
    }

    private void discover() {
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
                if (result == null
                        || result.getDevice() == null
                        || TextUtils.isEmpty(result.getDevice().getName()))
                    return;

                StringBuilder builder = new StringBuilder(result.getDevice().getName());

                //builder.append("\n").append(new String(result.getScanRecord().getServiceData(result.getScanRecord().getServiceUuids().get(0)), Charset.forName("UTF-8")));

                mText.setText(builder.toString());
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

        mBluetoothLeScanner.startScan(filters, settings, scanCallback);
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
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(pUuid)
                //.addServiceData(pUuid, "Data".getBytes(Charset.forName("UTF-8")))
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

        advertiser.startAdvertising(settings, data, advertisingCallback);
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
