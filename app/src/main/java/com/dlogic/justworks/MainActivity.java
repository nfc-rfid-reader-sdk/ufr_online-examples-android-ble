package com.dlogic.justworks;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBTAdapter;
    Button pairConnectBtn;
    Button beepBtn;
    Button scanBtn;
    CheckBox pairCheck;
    Spinner txtName;
    IntentFilter bleIntent = new IntentFilter();
    String deviceName = "";
    BluetoothGatt gatt;
    boolean connected = false;
    boolean justScan = false;
    private static final UUID serviceToWrite = UUID.fromString("e7f9840b-d767-4169-a3d0-a83b083669df");
    private static final UUID characteristicToWrite = UUID.fromString("8bdc835c-10fe-407f-afb0-b21926f068a7");

    BluetoothLeScanner btScanner;
    ArrayAdapter<String> spinnerAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pairConnectBtn = findViewById(R.id.pairConnectBtn);
        beepBtn = findViewById(R.id.beepBtn);
        txtName = findViewById(R.id.txtName);
        scanBtn = findViewById(R.id.scanBtn);
        beepBtn.setClickable(false);
        beepBtn.setAlpha(.3f);
        pairConnectBtn.setClickable(false);
        pairConnectBtn.setAlpha(.3f);

        pairCheck = findViewById(R.id.pairCheck);

        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTPermissions();

        bleIntent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(blReceiver, bleIntent);

        spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, android.R.id.text1);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        txtName.setAdapter(spinnerAdapter);

        pairConnectBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                        getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "You have to allow access to device location for BLE scan", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!mBTAdapter.isEnabled()) {
                    Toast.makeText(getApplicationContext(), "You have to turn Bluetooth ON", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    prepare();
                }
            }

        });

        scanBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                        getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "You have to allow access to device location for BLE scan", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!mBTAdapter.isEnabled()) {
                    Toast.makeText(getApplicationContext(), "You have to turn Bluetooth ON", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    scan();
                }
            }

        });

        beepBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                beep();
            }
        });
    }

    private void beep()
    {
        byte[] beepCmd = new byte[]{0x55, 0x26, (byte)0xAA, 0x00, 0x01, 0x02, (byte)0xE1};
        BluetoothGattService svc = gatt.getService(serviceToWrite);
        BluetoothGattCharacteristic chr = svc.getCharacteristic(characteristicToWrite);
        chr.setValue(beepCmd);
        gatt.writeCharacteristic(chr);
    }

    private void scan()
    {
        spinnerAdapter.clear();
        justScan = true;
        if (mBTAdapter.isEnabled()) {

                btScanner = mBTAdapter.getBluetoothLeScanner();
            //    btScanner.stopScan(leScanCallback);
                btScanner.startScan(leScanCallback);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.e("Scanning", "Scan stopped after 3s");
                    btScanner.stopScan(leScanCallback);
                    if(spinnerAdapter.getCount() == 0)
                    {
                        pairConnectBtn.setClickable(false);
                        pairConnectBtn.setAlpha(.3f);
                    }
                }
            }, 3000);
                Log.e("Scanning", "Scan started");

        } else {
            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
        }
    }

    private void prepare() {
        justScan = false;
        deviceName = txtName.getSelectedItem().toString();
        Log.e("Prepare", "Preparing");
        if (connected) {
            Log.e("Before scan", "Already connected - Disconnecting");
            gatt.disconnect();
            return;
        }
        if (mBTAdapter.isEnabled()) {
            boolean paired = false;
            for (BluetoothDevice device : mBTAdapter.getBondedDevices()) {
                if (device.getName().equals(deviceName)) {
                    Log.e("Before scan", "Already paired");
                    paired = true;
                    afterPair(device);
                    break;
                }
            }
            if (paired == false) {
                btScanner = mBTAdapter.getBluetoothLeScanner();

                //SCAN FILTER IS OPTIONAL
                List<ScanFilter> filters = new ArrayList<ScanFilter>();
                ScanFilter filter = new ScanFilter.Builder().setDeviceName(deviceName).build();
                filters.add(filter);

                //SCAN SETTINGS IS OPTIONAL
                ScanSettings settings = new ScanSettings.Builder().setScanMode(SCAN_MODE_LOW_LATENCY).build();
               // btScanner.stopScan(leScanCallback);
                btScanner.startScan(filters, settings, leScanCallback);
                Log.e("Scanning", "Scan started");
            }
        } else {
            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
        }
    }

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            try {
                BluetoothDevice device = result.getDevice();
                if(justScan)
                {
                   if(!checkDuplicate(device.getName()) && device.getName().startsWith("ON"))
                   {
                       Log.e("TAG", device.getName());
                       spinnerAdapter.add(device.getName());
                       spinnerAdapter.notifyDataSetChanged();
                       pairConnectBtn.setClickable(true);
                       pairConnectBtn.setAlpha(1f);
                   }

                }
                else {
                    if (device.getName().equals(deviceName)) {
                        Log.e("Scanning", "Device found");
                        btScanner.stopScan(leScanCallback);
                        if (pairCheck.isChecked()) {
                            device.createBond();
                            pairConnectBtn.setText("Pairing");
                        } else {
                            afterPair(device);
                        }


                    }
                }
            } catch (Exception ex) {
            }
        }
    };

    private boolean checkDuplicate(String name)
    {
        for (int i = 0; i < spinnerAdapter.getCount(); i++)
        {
            if(spinnerAdapter.getItem(i).equals(name))
            {
                return true;
            }
        }
        return false;
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                try {
                    if (device.getName().equals(deviceName) && device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        Log.e("Pairing", "Device paired");
                        afterPair(device);
                    }
                } catch (Exception ex) {
                }
            }

        }
    };

    private void afterPair(BluetoothDevice device) {
        if (connected == false)
            gatt = device.connectGatt(getApplicationContext(), false, bleGattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    private final BluetoothGattCallback bleGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt.close();
                Log.e("GATT", "Disconnected");
                pairConnectBtn.setText("Connect");
                connected = false;
                beepBtn.setClickable(false);
                beepBtn.setAlpha(.3f);
            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                Log.e("GATT", "Connecting");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                Log.e("GATT", "Disconnecting");
            }
            else
            {
                Log.e("GATT", "Error");
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if(status == BluetoothGatt.GATT_SUCCESS)
            {
                Log.e("GATT", "Connected");
                pairConnectBtn.setText("Connected");
                connected = true;
                beepBtn.setClickable(true);
                beepBtn.setAlpha(1f);
            }
            else
            {
                Log.e("GATT", "Service discovering error");
            }

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

    };

    private void checkBTPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(blReceiver);
    }
}