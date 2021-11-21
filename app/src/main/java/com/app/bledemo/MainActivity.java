package com.app.bledemo;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_POWER;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private BluetoothAdapter mBluetoothAdapter;
    private static final String BLE_UNO_UUID = "0000dfb0-0000-1000-8000-00805f9b34fb";
    private static final String BLE_UNO_DATA_UUID = "0000dfb1-0000-1000-8000-00805f9b34fb";
    BluetoothLeScanner bluetoothLeScanner;
    private BluetoothDevice mBleDevice;
    private BluetoothGatt mGatt;

    private TextView mTvName;
    private TextView mTvAddress;

    private TextView tvMsg;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "你的设备不支持蓝牙!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            new RxPermissions(this).request(Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    .subscribe(granted -> {
                        if (granted) {
                            //权限允许成功
                            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                            if (!mBluetoothAdapter.isEnabled()) {
                                //蓝牙未开启
                                mBluetoothAdapter.enable();
                            } else {
                                //开始扫描蓝牙设备
                                scanBleDevice();
                            }
                        } else {
                            Toast.makeText(this, "无权限!", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void initView() {
        mTvName = findViewById(R.id.tvName);
        mTvAddress = findViewById(R.id.tvAddress);
        tvMsg = findViewById(R.id.tvMsg);
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch sw = findViewById(R.id.switch1);
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                BluetoothGattService service = mGatt.getService(UUID.fromString(BLE_UNO_UUID));
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(BLE_UNO_DATA_UUID));
                characteristic.setValue(isChecked ? "open" : "close");
                mGatt.writeCharacteristic(characteristic);
                Toast.makeText(this, "打开成功", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "关闭失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        sw.setChecked(false);
    }

    /**
     * 开始扫描蓝牙设备
     */
    private void scanBleDevice() {
        bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        //添加搜索条件，按照固定的UUID产品类型搜索
        List<ScanFilter> filters = new ArrayList<>();
        ParcelUuid uuid = ParcelUuid.fromString(BLE_UNO_UUID);
        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(uuid)
                .build();
        filters.add(scanFilter);
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(SCAN_MODE_LOW_POWER)
                .build();

        //开始扫描设备
        bluetoothLeScanner.startScan(filters, scanSettings, scanCallback);
    }

    /**
     * 低功耗蓝牙扫描回调
     */
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.i(TAG, "threadName =====> " + Thread.currentThread().getName());
            if (mBleDevice == null) {
                mBleDevice = mBluetoothAdapter.getRemoteDevice(result.getDevice().getAddress());
                String deviceName = mBleDevice.getName();
                String address = mBleDevice.getAddress();
                Log.i(TAG, "deviceName:" + deviceName + ", address:" + address);
                mTvName.setText(deviceName);
                mTvAddress.setText(address);
            } else {
                bluetoothLeScanner.stopScan(this);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            bluetoothLeScanner.stopScan(this);
        }
    };

    /**
     * 蓝牙连接监听
     */
    private final BluetoothGattCallback connCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.i(TAG, "onConnectionStateChange status =====> " + newState);
            switch (newState) {
                case BluetoothGatt.STATE_CONNECTED:
                    gatt.discoverServices();
                    break;
                case BluetoothGatt.GATT_FAILURE:
                    gatt.close();
                    mGatt = null;
                    MainActivity.this.runOnUiThread(() -> Toast.makeText(MainActivity.this, "连接失败!", Toast.LENGTH_SHORT).show());
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.i(TAG, "onServicesDiscovered status =====> " + status);
            switch (status) {
                case BluetoothGatt.GATT_SUCCESS:
                    //此时可以去找特征值了
                    mGatt = gatt;
                    MainActivity.this.runOnUiThread(() -> Toast.makeText(MainActivity.this, "连接成功!", Toast.LENGTH_SHORT).show());
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.i(TAG, "onCharacteristicRead status =====> " + status);

//            BluetoothGattService service = gatt.getService(UUID.fromString(BLE_UNO_UUID));
//            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(BLE_UNO_DATA_UUID));
//            characteristic.setValue(mEtContent.getText().toString());
            String str = new String(characteristic.getValue(), StandardCharsets.UTF_8);
            tvMsg.setText(str);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.i(TAG, "onCharacteristicWrite status =====> " + status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.i(TAG, "onCharacteristicChanged");
        }
    };

    /**
     * 连接蓝牙设备
     *
     * @param view
     */
    public void conn(View view) {
        if (mBleDevice == null) {
            Toast.makeText(this, "无设备", Toast.LENGTH_SHORT).show();
            return;
        }
        mBleDevice.connectGatt(this, false, connCallback);
    }

}