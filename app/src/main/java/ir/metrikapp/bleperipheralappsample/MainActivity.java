package ir.metrikapp.bleperipheralappsample;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import ir.metrikapp.bleperipheralappsample.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final UUID calculationServiceUuid = UUID.randomUUID();
    private final UUID characteristicWriteUuid = UUID.randomUUID();
    private final UUID characteristicNotifyUuid = UUID.randomUUID();
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
    private BluetoothGattServer mGattServer;
    private BluetoothGattCharacteristic mWriteCharacteristic;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothLeAdvertiser advertiser;
    private AdvertiseCallback advertisingCallback;

    private BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "onConnectionStateChange: " + status, Toast.LENGTH_SHORT).show());

        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "onServiceAdded: " + service.toString(), Toast.LENGTH_SHORT).show());
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "onCharacteristicReadRequest: " + characteristic.toString(), Toast.LENGTH_SHORT).show());
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
//            runOnUiThread(() -> Toast.makeText(MainActivity.this, "onCharacteristicWriteRequest: " + Arrays.toString(value), Toast.LENGTH_SHORT).show());

            String message = new String(value, StandardCharsets.UTF_8);
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "onCharacteristicWriteRequest: " + message, Toast.LENGTH_SHORT).show());

            String result;

            if (message.contains("+")) {
                String[] operands = message.split("\\+");
                try {
                    result = String.valueOf(Integer.parseInt(operands[0]) + Integer.parseInt(operands[1]));
                } catch (Exception ex) {
                    result = "ERROR";
                }
            } else if (message.contains("-")) {
                String[] operands = message.split("-");
                try {
                    result = String.valueOf(Integer.parseInt(operands[0]) - Integer.parseInt(operands[1]));
                } catch (Exception ex) {
                    result = "ERROR";
                }
            } else if (message.contains("*")) {
                String[] operands = message.split("\\*");
                try {
                    result = String.valueOf(Integer.parseInt(operands[0]) * Integer.parseInt(operands[1]));
                } catch (Exception ex) {
                    result = "ERROR";
                }
            } else if (message.contains("/")) {
                String[] operands = message.split("/");
                try {
                    result = String.valueOf(Integer.parseInt(operands[0]) / Integer.parseInt(operands[1]));
                } catch (Exception ex) {
                    result = "ERROR";
                }
            } else {
                result = "ERROR";
            }
            
            mNotifyCharacteristic.setValue(result);

            mGattServer.notifyCharacteristicChanged(device, mNotifyCharacteristic, false);

            if (responseNeeded) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "onDescriptorReadRequest: " + descriptor.toString(), Toast.LENGTH_SHORT).show());
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.getValue());
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "onDescriptorWriteRequest: " + Arrays.toString(value), Toast.LENGTH_SHORT).show());
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            super.onExecuteWrite(device, requestId, execute);
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "onExecuteWrite", Toast.LENGTH_SHORT).show());
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "onNotificationSent: " + status, Toast.LENGTH_SHORT).show());
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
        }

        @Override
        public void onPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(device, txPhy, rxPhy, status);
        }

        @Override
        public void onPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status) {
            super.onPhyRead(device, txPhy, rxPhy, status);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(activityMainBinding.getRoot());

        Button mAdvertiseButton = findViewById(R.id.advertise_btn);

        mAdvertiseButton.setOnClickListener(this);

        init();
        setGattServer();
        setBluetoothService();
    }

    @Override
    public void onClick(View v) {
        startAdvertising();
    }

    private void startAdvertising() {
        advertiser = bluetoothService.getAdapter().getBluetoothLeAdvertiser();
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build();

        ParcelUuid pUuid = new ParcelUuid(calculationServiceUuid);

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(pUuid)
//                .addServiceData(pUuid, "Data".getBytes(StandardCharsets.UTF_8))
                .build();

        advertisingCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Toast.makeText(MainActivity.this, "Advertising onStartSuccess", Toast.LENGTH_SHORT).show();
                super.onStartSuccess(settingsInEffect);
            }

            @Override
            public void onStartFailure(int errorCode) {
                String description;
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

    private void setGattServer() {
        mGattServer = bluetoothService.openGattServer(this, bluetoothGattServerCallback);
    }

    private void setBluetoothService() {

        // create the Service
        BluetoothGattService mSampleService = new BluetoothGattService(calculationServiceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        /*
        create the Characteristic.
        we need to grant to the Client permission to read (for when the user clicks the "Request Characteristic" button).
        no need for notify permission as this is an action the Server initiate.
         */
        mWriteCharacteristic = new BluetoothGattCharacteristic(characteristicWriteUuid, BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);
        mNotifyCharacteristic = new BluetoothGattCharacteristic(characteristicNotifyUuid, BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ);
//        setCharacteristic(); // set initial state
        // add the Characteristic to the Service
        mSampleService.addCharacteristic(mWriteCharacteristic);
        mSampleService.addCharacteristic(mNotifyCharacteristic);

        // add the Service to the Server/Peripheral
        if (mGattServer != null) {
            mGattServer.addService(mSampleService);
        }
    }

    private void stopAdvertising() {
        if (advertiser != null) {
            advertiser.stopAdvertising(advertisingCallback);
            advertisingCallback = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAdvertising();
    }
}