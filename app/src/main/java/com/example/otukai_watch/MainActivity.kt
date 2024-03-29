package com.example.otukai_watch

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.otukai_watch.ToDoList.ItemActivity
import com.example.otukai_watch.ToDoList.Task
import com.example.otukai_watch.VoiceChat.VoiceActivity
import com.github.kittinunf.fuel.httpGet
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.android.synthetic.main.activity_item.*
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var textMessage: TextView
    private val onNavigationItemSelectedListener =
        BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    return@OnNavigationItemSelectedListener true
                }
                R.id.navigation_dashboard -> {
                    val intent = Intent(this, VoiceActivity::class.java)
                    startActivity(intent)
                }
                R.id.navigation_notifications -> {
                    val intent = Intent(this, ItemActivity::class.java)
                    startActivity(intent)
                }
            }
            false
        }

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {

        // API URL
        val requestUrl = "https://pck.itok01.com/api/v1/location"

        mMap = googleMap

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

        val httpAsync = (requestUrl + "?user=taro")
            .httpGet()
            .responseString { request, response, result ->
                Log.d("hoge", result.toString())
                when (result) {
                    is Result.Failure -> {
                        val ex = result.getException()
                        println(ex)
                    }
                    is Result.Success -> {
                        val data = result.get()
                        println(data)
                        val pos = moshi.adapter(Position::class.java).fromJson(data)

                        //val basyo = LatLng(pos!!.latitude, pos!!.longitude) //緯度,経度
                        val basyo = LatLng(pos!!.latitude, pos!!.longitude) //緯度,経度
                        val zoomValue = 13.0f // 1.0f 〜 21.0f を指定
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(basyo, zoomValue))
                        //   mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(basyo, zoomValue))

                        val marker = mMap.addMarker(
                            MarkerOptions()
                                .position(basyo)
                                .title("現在地")    // マーカーをタップ時に表示するテキスト文字列
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                        marker.showInfoWindow() // タップした時と同じ挙動
                    }
                }
            }

        httpAsync.join()


    }

    data class Position(val user: String, val latitude: Double, val longitude: Double)


}
