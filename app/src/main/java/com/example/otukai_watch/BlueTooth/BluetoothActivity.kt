package com.example.otukai_watch.BlueTooth

import androidx.appcompat.app.AppCompatActivity
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import com.example.otukai_watch.R
import android.widget.Toast
import sun.util.locale.provider.LocaleProviderAdapter.getAdapter
import android.content.Context.BLUETOOTH_SERVICE
import androidx.core.content.ContextCompat.getSystemService
import android.bluetooth.BluetoothManager
import android.content.Context




class BluetoothActivity  : AppCompatActivity() {

    private val REQUEST_ENABLEBLUETOOTH = 1 // Bluetooth機能の有効化要求時の識別コード
    private var mBluetoothAdapter: BluetoothAdapter? = null    // BluetoothAdapter : Bluetooth処理で必要

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)
    }


}
