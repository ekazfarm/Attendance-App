package com.ekawafers.attandanceapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_dialog_form.view.*
import java.lang.Math.toRadians
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*


class MainActivity : AppCompatActivity() {

    companion object {
        const val ID_LOCATION_PERMISSION = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermisionLocation()
        onClick()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ID_LOCATION_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED ||
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Berhasi di Ijinkan", Toast.LENGTH_SHORT).show()

                if (!isLocationEnabled()){
                    Toast.makeText(this, "Tolong aktifkan lokasi anda", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            }else{
                Toast.makeText(this, "Gagal di Ijinkan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermisionLocation() {
      if (checkPermission()){
          if (!isLocationEnabled()){
              Toast.makeText(this, "Tolong aktifkan lokasi anda", Toast.LENGTH_SHORT).show()
              startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
          }
      }else{
          requestPermission()
      }
    }
    private fun checkPermission(): Boolean{
        if (ActivityCompat.checkSelfPermission( this,
            Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        return false
    }

    private fun isLocationEnabled(): Boolean{
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return true
        }
        return false
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            ID_LOCATION_PERMISSION
        )
    }


    private fun onClick() {
        fabAbsen.setOnClickListener{
            loadScanLocation()
            Handler().postDelayed({
                getLastLocation()
            }, 6000)
        }
    }

    private fun loadScanLocation(){
        ripplebackground.startRippleAnimation()
        tvscanning.visibility = View.VISIBLE
        tvabsensukses.visibility = View.GONE
    }
    private fun stopScanLocation(){
        ripplebackground.stopRippleAnimation()
        tvscanning.visibility = View.GONE
    }

    private fun getLastLocation(){
        if (checkPermission()){
            if (isLocationEnabled()){
                LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener {location ->
                    val currentLat = location.latitude
                    val currentLong = location.longitude





                 //untuk mengecek logcat latitude dan longtitud
                // Log.d("coba", "size: ${getAddresses().size}")
                //    for (address: Address in getAddresses()) {
                 //       Log.d("coba", "lat: ${address.latitude}, lon: ${address.longitude}")
                //    }


                    val distance = calculateDistance(
                        currentLat,
                        currentLong,
                        getAddresses()[0].latitude,
                        getAddresses()[0].longitude) * 1000

                        if (distance < 40.0) {
                            showDialogForm()
                        }else{
                            tvabsensukses.visibility = View.VISIBLE
                            tvabsensukses.text = "Anda Terlalu jauh dari kantor"

                        }

        
                    //Log.d("coba", "curent location: $currentLat, $currentLong")
                   // tvabsensukses.visibility = View.VISIBLE
                   // tvabsensukses.text = "distance: $distance"

                    stopScanLocation()
                }
            }else{
                Toast.makeText(this, "Tolong aktifkan lokasi anda", Toast.LENGTH_SHORT).show()
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        }else{
            requestPermission()
        }
    }

    private fun showDialogForm() {
        val dialogForm = LayoutInflater.from(this).inflate(R.layout.layout_dialog_form, null)
        AlertDialog.Builder(this)
            .setView(dialogForm)
            .setCancelable(false)
            .setPositiveButton("Submit") {  dialog, _ ->
                val name = dialogForm.etNama.text.toString()
                inputDataToFireBase(name)
               // Toast.makeText(this, "Nama: $name", Toast.LENGTH_SHORT).show()

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()

            }
            .show()
    }

    private fun inputDataToFireBase(name: String) {
        val user = User(name, getCurrentDate())

        val database = FirebaseDatabase.getInstance()
        val attendanceRef = database.getReference("log_attendance")

        attendanceRef.child(name).setValue(user)
            .addOnSuccessListener {
                tvabsensukses.visibility = View.VISIBLE
                tvabsensukses.text = "Absen Sukses"
            }
            .addOnFailureListener {
                Toast.makeText(this, "${it.message}", Toast.LENGTH_SHORT).show()
            }

    }

    private fun getCurrentDate(): String{
        val  currentTime = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(currentTime)
    }

    private fun getAddresses(): List<Address>{
        val destinationPlaces = "PT Maxindo Mitra Solusi - Network Operator Center"
        val geocode = Geocoder(this, Locale.getDefault())
        return geocode.getFromLocationName(destinationPlaces, 100)
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6372.8 // in kilometers
        val radiansLat1 = toRadians(lat1)
        val radiansLat2 = toRadians(lat2)
        val dLat = toRadians(lat2 - lat1)
        val dLon = toRadians(lon2 - lon1)
        return 2 * r * asin(sqrt(sin(dLat / 2).pow(2.0) + sin(dLon / 2).pow(2.0) * cos(radiansLat1) * cos(radiansLat2)))
    }

}