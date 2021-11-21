package com.app.bledemo;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

public class BrodCastActivity extends AppCompatActivity implements View.OnClickListener {


    public final static UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_LOST_SERVICE = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_LOST_WRITE = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_LOST_ENABLE = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");

    private static final String TAG = "yushu";
    private TextView txtDevice;
    private Button btnStart;
    private Button btnNotify;
    private Button btnStop;

    private BluetoothManager mBluetoothManager;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    private BluetoothGattServer gattServer;
    private BluetoothGattCharacteristic characterNotify, characterWrite;
    private BluetoothDevice bluetoothDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brod);
        initView();
        initBLE();
        setServer();
    }

    private void initView() {
        txtDevice = (TextView) findViewById(R.id.txt_device);
        btnStart = (Button) findViewById(R.id.btn_start);
        btnNotify = (Button) findViewById(R.id.btn_notify);
        btnStop = (Button) findViewById(R.id.btn_stop);
        btnNotify.setOnClickListener(this);
        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
    }

    /**
     * 初始化蓝牙
     */
    private void initBLE() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "不支持BLE", Toast.LENGTH_LONG).show();
            finish();
        }
        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "蓝牙不支持", Toast.LENGTH_LONG).show();
            finish();
        }
        assert mBluetoothAdapter != null;
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            Toast.makeText(this, "the device not support peripheral", Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    /**
     * 添加服务，特征
     */
    private void setServer() {
        //读写特征
        characterWrite = new BluetoothGattCharacteristic(
                UUID_LOST_WRITE, BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        //使能特征
        characterNotify = new BluetoothGattCharacteristic(UUID_LOST_ENABLE,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ);
        characterNotify.addDescriptor(new BluetoothGattDescriptor(CLIENT_CHARACTERISTIC_CONFIG, BluetoothGattDescriptor.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ));
        //服务
        BluetoothGattService gattService = new BluetoothGattService(UUID_LOST_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        //为服务添加特征
        gattService.addCharacteristic(characterWrite);
        gattService.addCharacteristic(characterNotify);
        //管理服务，连接和数据交互回调
        gattServer = mBluetoothManager.openGattServer(this,
                new BluetoothGattServerCallback() {

                    @Override
                    public void onConnectionStateChange(final BluetoothDevice device,
                                                        final int status, final int newState) {
                        super.onConnectionStateChange(device, status, newState);
                        bluetoothDevice = device;
                        Log.d("Chris", "onConnectionStateChange:" + device + "    " + status + "   " + newState);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                txtDevice.setText(device.getAddress() + "   " + device.getName() + "   " + status + "  " + newState);
                            }
                        });
                    }

                    @Override
                    public void onServiceAdded(int status,
                                               BluetoothGattService service) {
                        super.onServiceAdded(status, service);
                        Log.d("Chris", "service added");
                    }

                    @Override
                    public void onCharacteristicReadRequest(
                            BluetoothDevice device, int requestId, int offset,
                            BluetoothGattCharacteristic characteristic) {
                        super.onCharacteristicReadRequest(device, requestId,
                                offset, characteristic);
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
                        Log.d("Chris", "onCharacteristicReadRequest");
                    }

                    @Override
                    public void onCharacteristicWriteRequest(
                            BluetoothDevice device, int requestId,
                            BluetoothGattCharacteristic characteristic,
                            boolean preparedWrite, boolean responseNeeded,
                            int offset, final byte[] value) {
                        super.onCharacteristicWriteRequest(device, requestId,
                                characteristic, preparedWrite, responseNeeded,
                                offset, value);
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
                        Log.d("Chris", "onCharacteristicWriteRequest" + value[0]);
                        runOnUiThread(new Runnable() {
                            @SuppressLint("SetTextI18n")
                            @Override
                            public void run() {
                                txtDevice.setText(value[0] + "");
                            }
                        });
                    }

                    @Override
                    public void onNotificationSent(BluetoothDevice device, int status) {
                        super.onNotificationSent(device, status);
                        Log.i(TAG, "onNotificationSent: ");
                    }

                    @Override
                    public void onMtuChanged(BluetoothDevice device, int mtu) {
                        super.onMtuChanged(device, mtu);
                    }

                    @Override
                    public void onDescriptorReadRequest(BluetoothDevice device,
                                                        int requestId, int offset,
                                                        BluetoothGattDescriptor descriptor) {
                        super.onDescriptorReadRequest(device, requestId,
                                offset, descriptor);
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characterNotify.getValue());
                        Log.d("Chris", "onDescriptorReadRequest");
                    }

                    @Override
                    public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                                         BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded,
                                                         int offset, byte[] value) {
                        super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded,
                                offset, value);
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
//                        characterNotify.setValue("HIHHHHH");
//                        gattServer.notifyCharacteristicChanged(bluetoothDevice, characterNotify, false);
                        Log.d("Chris", "onDescriptorWriteRequest");
                    }

                    @Override
                    public void onExecuteWrite(BluetoothDevice device,
                                               int requestId, boolean execute) {
                        super.onExecuteWrite(device, requestId, execute);
                        Log.d("Chris", "onExecuteWrite");
                    }
                });
        gattServer.addService(gattService);
    }

    /**
     * 广播的一些基本设置
     **/
    public AdvertiseSettings createAdvSettings(boolean connectAble, int timeoutMillis) {
        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
        builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        builder.setConnectable(connectAble);
        builder.setTimeout(timeoutMillis);
        builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        AdvertiseSettings mAdvertiseSettings = builder.build();
        if (mAdvertiseSettings == null) {
            Toast.makeText(this, "mAdvertiseSettings == null", Toast.LENGTH_LONG).show();
            Log.e(TAG, "mAdvertiseSettings == null");
        }
        return mAdvertiseSettings;
    }

    @Override
    public void onClick(View v) {
        if (v == btnNotify) {
            characterNotify.setValue("HIHHHHH");
            gattServer.notifyCharacteristicChanged(bluetoothDevice, characterNotify, false);
        } else if (v == btnStart) {
            mBluetoothLeAdvertiser.startAdvertising(createAdvSettings(true, 0), createAdvertiseData(), mAdvertiseCallback);
        } else if (v == btnStop) {
            stopAdvertise();
        }
    }

    private void stopAdvertise() {
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        }
    }

    //广播数据
    public AdvertiseData createAdvertiseData() {
        AdvertiseData.Builder mDataBuilder = new AdvertiseData.Builder();
        mDataBuilder.setIncludeDeviceName(true); //广播名称也需要字节长度
        mDataBuilder.setIncludeTxPowerLevel(true);
        mDataBuilder.addServiceData(ParcelUuid.fromString("0000fff0-0000-1000-8000-00805f9b34fb"), new byte[]{1, 2});
        AdvertiseData mAdvertiseData = mDataBuilder.build();
        if (mAdvertiseData == null) {
            Toast.makeText(this, "mAdvertiseSettings == null", Toast.LENGTH_LONG).show();
            Log.e(TAG, "mAdvertiseSettings == null");
        }
        return mAdvertiseData;
    }


    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            if (settingsInEffect != null) {
                Log.d(TAG, "onStartSuccess TxPowerLv=" + settingsInEffect.getTxPowerLevel() + " mode=" + settingsInEffect.getMode()
                        + " timeout=" + settingsInEffect.getTimeout());
            } else {
                Log.e(TAG, "onStartSuccess, settingInEffect is null");
            }
            Log.e(TAG, "onStartSuccess settingsInEffect： " + settingsInEffect);

        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.e(TAG, "onStartFailure errorCode" + errorCode);

            if (errorCode == ADVERTISE_FAILED_DATA_TOO_LARGE) {
                Toast.makeText(BrodCastActivity.this, "R.string.advertise_failed_data_too_large", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to start advertising as the advertise data to be broadcasted is larger than 31 bytes.");
            } else if (errorCode == ADVERTISE_FAILED_TOO_MANY_ADVERTISERS) {
                Toast.makeText(BrodCastActivity.this, "R.string.advertise_failed_too_many_advertises", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to start advertising because no advertising instance is available.");
            } else if (errorCode == ADVERTISE_FAILED_ALREADY_STARTED) {
                Toast.makeText(BrodCastActivity.this, "R.string.advertise_failed_already_started", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to start advertising as the advertising is already started");
            } else if (errorCode == ADVERTISE_FAILED_INTERNAL_ERROR) {
                Toast.makeText(BrodCastActivity.this, "R.string.advertise_failed_internal_error", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Operation failed due to an internal error");
            } else if (errorCode == ADVERTISE_FAILED_FEATURE_UNSUPPORTED) {
                Toast.makeText(BrodCastActivity.this, "R.string.advertise_failed_feature_unsupported", Toast.LENGTH_LONG).show();
                Log.e(TAG, "This feature is not supported on this platform");
            }
        }
    };
}
