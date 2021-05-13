package ir.metrikapp.bleperipheralappsample;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import java.util.UUID;

import ir.metrikapp.bleperipheralappsample.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ActivityMainBinding activityMainBinding;
    private final ActivityResultLauncher<String> requestLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    enableNavigation();
                } else {
                    showErrorText("Location permission needed.");
                }
            });
    private BluetoothManager bluetoothService;
    private String uniqueId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(activityMainBinding.getRoot());

        Button mAdvertiseButton = findViewById(R.id.advertise_btn);

        mAdvertiseButton.setOnClickListener(this);

        uniqueId = UUID.randomUUID().toString();

        init();
    }

    @Override
    public void onClick(View v) {
        startAdvertising();
    }

    private void startAdvertising() {
        BluetoothLeAdvertiser advertiser = bluetoothService.getAdapter().getBluetoothLeAdvertiser();
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build();

        ParcelUuid pUuid = new ParcelUuid(UUID.fromString(uniqueId));

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(pUuid)
//                .addServiceData(pUuid, "Data".getBytes(StandardCharsets.UTF_8))
                .build();

        AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Toast.makeText(MainActivity.this, "Advertising onStartSuccess", Toast.LENGTH_SHORT).show();
                super.onStartSuccess(settingsInEffect);
            }

            @Override
            public void onStartFailure(int errorCode) {
                String description = "";
                if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED)
                    description = "ADVERTISE_FAILED_FEATURE_UNSUPPORTED";
                else if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS)
                    description = "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS";
                else if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED)
                    description = "ADVERTISE_FAILED_ALREADY_STARTED";
                else if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE)
                    description = "ADVERTISE_FAILED_DATA_TOO_LARGE";
                else if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR)
                    description = "ADVERTISE_FAILED_INTERNAL_ERROR";
                else description = "unknown";
                Toast.makeText(MainActivity.this, "Advertising onStartFailure: " + description, Toast.LENGTH_SHORT).show();
                super.onStartFailure(errorCode);
            }
        };

        advertiser.startAdvertising(settings, data, advertisingCallback);
    }

    private void init() {
        bluetoothService = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE));

        if (bluetoothService != null) {
            BluetoothAdapter mBluetoothAdapter = bluetoothService.getAdapter();
            // Is Bluetooth supported on this device?
            if (mBluetoothAdapter != null) {
                // Is Bluetooth turned on?
                if (mBluetoothAdapter.isEnabled()) {
                    // Are Bluetooth Advertisements supported on this device?
                    if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
                            } else {
                                // Everything is supported and enabled.
                                enableNavigation();
                            }
                        } else {
                            // Everything is supported and enabled.
                            enableNavigation();
                        }
                    } else {
                        // Bluetooth Advertisements are not supported.
                        showErrorText("Bluetooth Advertisements are not supported.");
                    }
                } else {
                    // Turn on Bluetooth.
                    mBluetoothAdapter.enable();
                    init();
                }
            } else {
                // Bluetooth is not supported.
                showErrorText("Bluetooth is not supported.");
            }
        }
    }

    private void enableNavigation() {
        showErrorText("Everything done!");
    }

    private void showErrorText(String errorMessage) {
        Snackbar.make(activityMainBinding.getRoot(), errorMessage, Snackbar.LENGTH_LONG).show();
    }
}