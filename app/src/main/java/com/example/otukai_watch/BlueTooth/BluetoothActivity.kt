package com.example.otukai_watch.BlueTooth

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chootdev.recycleclick.RecycleClick
import com.example.otukai_watch.R
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_bluetooth.*

class BluetoothActivity : AppCompatActivity() {

  private val REQUEST_ENABLE_BT = 0
  private val REQUEST_DISCOVER_BT = 1
  private var STORAGE_PERMISSION = 9652
  private var ACCESS_COARSE_LOCATION_PERMISSION = 9651
  lateinit var mBlueAdapter: BluetoothAdapter
  private var bluetoothDevices: ArrayList<BluetoothDevice> = arrayListOf()
  var list: ArrayList<DeviceInfo> = arrayListOf()
  private var havePermissions = false
  lateinit var mBluetoothConnection: BluetoothConnectionService
  lateinit var rxPermissions: RxPermissions
  lateinit var progressDialog: ProgressDialog

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_bluetooth)

    val builder = StrictMode.VmPolicy.Builder()
    StrictMode.setVmPolicy(builder.build())

    progressDialog = ProgressDialog(this@BluetoothActivity)
    progressDialog.setTitle("おつかいウォッチを検索中...")
    progressDialog.setMessage("Please wait...")

    //adapter
    mBlueAdapter = BluetoothAdapter.getDefaultAdapter()

    rxPermissions = RxPermissions(this)

    mBluetoothConnection = BluetoothConnectionService(this@BluetoothActivity)
    mBluetoothConnection.mBluetoothAdapter = mBlueAdapter

    checkPermissions()

    val linearLayoutManager =
      LinearLayoutManager(this@BluetoothActivity, RecyclerView.VERTICAL, false)
    recyclerViewPairedDevices.layoutManager = linearLayoutManager

    scanDevices()

    //check if bluetooth is available or not
    if (mBlueAdapter == null) {
      statusBluetoothTv.text = "Bluetoothは利用できません"
    } else {
      statusBluetoothTv.text = "Bluetoothを利用できます"
    }

    //set image according to bluetooth status(on/off)
    if (mBlueAdapter.isEnabled) {
      bluetoothIv.setImageResource(R.drawable.bluetooth)
    } else {
      bluetoothIv.setImageResource(R.drawable.bluetooth_disabled)
    }

    mBluetoothConnection.messageLiveData.observe(this, androidx.lifecycle.Observer {
      Toast.makeText(this@BluetoothActivity, "Text ->>> $it", Toast.LENGTH_SHORT).show()
    })

    onBtn.setOnClickListener {
      if (!mBlueAdapter.isEnabled) {
        showToast("Bluetoothを起動してます....")
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(intent, REQUEST_ENABLE_BT)
      } else {
        showToast("Bluetoothは既に起動されてます")
      }
    }



    discoverableBtn.setOnClickListener {
      if (mBlueAdapter.isEnabled) {
        mBlueAdapter.disable()
        showToast("Bluetoothを停止しています...")
        bluetoothIv.setImageResource(R.drawable.bluetooth_disabled)
      } else {
        showToast("Bluetoothは既に停止されています")
      }
    }

    btnScan.setOnClickListener {
      btnScan.isEnabled = false
      scanDevices()
      btnScan.isEnabled = true
    }



    RecycleClick.addTo(recyclerViewPairedDevices).setOnItemClickListener { recyclerView, position, v ->
      if (list[position].isHeader.not()){
        showToast(list[position].deviceName + ", " + list[position].deviceAddress)
        val filteredList = bluetoothDevices.filter {it.address.equals(list[position].deviceAddress)}
        if (filteredList.isNotEmpty()) {
          mBluetoothConnection.startClient(filteredList[0], BluetoothConnectionService.MY_UUID_INSECURE)
        }
      }
    }

  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == REQUEST_ENABLE_BT) {
      if (resultCode === Activity.RESULT_OK) {
        //bluetooth is on
        bluetoothIv.setImageResource(R.drawable.bluetooth)
        showToast("Bluetoothは起動しています")
      } else {
        //user denied to turn bluetooth on
        showToast("Bluetoothを起動できませんでした")
      }
    }
    super.onActivityResult(requestCode, resultCode, data)
  }

  private fun checkPermissions() {
    if (ContextCompat.checkSelfPermission(
        this@BluetoothActivity,
        Manifest.permission.READ_EXTERNAL_STORAGE
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      if (ActivityCompat.shouldShowRequestPermissionRationale(
          this,
          Manifest.permission.READ_EXTERNAL_STORAGE
        )
      ) {
        showToast("Permission Deined")
        havePermissions = false
      } else {
        ActivityCompat.requestPermissions(
          this,
          arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
          ),
          STORAGE_PERMISSION
        )
        havePermissions = false
      }

    } else {
      havePermissions = true
    }


    if (ContextCompat.checkSelfPermission(
        this@BluetoothActivity,
        Manifest.permission.ACCESS_COARSE_LOCATION
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      if (ActivityCompat.shouldShowRequestPermissionRationale(
          this,
          Manifest.permission.ACCESS_COARSE_LOCATION
        )
      ) {
        showToast("Permission Deined")
        havePermissions = false
      } else {
        ActivityCompat.requestPermissions(
          this,
          arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION
          ),
          ACCESS_COARSE_LOCATION_PERMISSION
        )
        havePermissions = false
      }

    } else {
      havePermissions = true
    }

  }

  override fun onPause() {
    super.onPause()
    mBlueAdapter.cancelDiscovery()
  }

  private fun scanDevices(){
    progressDialog = ProgressDialog(this@BluetoothActivity)
    progressDialog.setTitle("おつかいウォッチを検索中...")
    progressDialog.setMessage("Please wait...")
    progressDialog.show()

    val broadCastReceiver = object : BroadcastReceiver() {
      override fun onReceive(contxt: Context?, intent: Intent?) {
        if (BluetoothDevice.ACTION_FOUND.equals(intent?.action)) {
          val device: BluetoothDevice? = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
          Log.d("testldfbnfdnbnd", device.toString())
          if (device != null) {
            list.add(DeviceInfo(device.name, device.address, "", false))
            bluetoothDevices.add(device)
          }
        }
      }
    }

    if (mBlueAdapter.isEnabled) {
      bluetoothDevices = ArrayList(mBlueAdapter.bondedDevices)
      list.add(DeviceInfo("", "", "接続したことがあるデバイス", true))
      bluetoothDevices.forEach {
        list.add(DeviceInfo(it.name, it.address, "", false))
      }
      list.add(DeviceInfo("", "", "利用可能なデバイス", true))

      val adapter = DeviceAdapter(list)
      recyclerViewPairedDevices.adapter = adapter


      val filter: IntentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
      registerReceiver(broadCastReceiver, filter)

      mBlueAdapter.startDiscovery()
      progressDialog.cancel()
    } else {
      //bluetooth is off so can't get paired devices
      showToast("BluetoothをONにしてこれまでにペアリングされたデバイスを取得します")
      progressDialog.cancel()
    }
  }



  //toast message function
  private fun showToast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
  }

}
