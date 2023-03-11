package my.edu.tarc.googlemap

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.util.*

class MainActivity() : AppCompatActivity(), OnMapReadyCallback {

    var isPermissionGranter: Boolean = false
    lateinit var gMap: GoogleMap
    lateinit var locationRequest: LocationRequest
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val imageButtonSearch: ImageButton = findViewById(R.id.imageButtonSearch)
        val editTextSearch: EditText = findViewById(R.id.editTextSearch)
        val supportMapFragment = SupportMapFragment.newInstance()

        supportFragmentManager.beginTransaction().add(R.id.fragmentMap, supportMapFragment)
            .commit()
        supportMapFragment.getMapAsync(this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        //checkPermission()     //will be trigger by “VIEW LOCATION” btn
        openGps()   //show dialog to open GPS   //will be trigger by “VIEW LOCATION” btn

        //For location searching
        imageButtonSearch.setOnClickListener {
            if(editTextSearch.text.isNotEmpty()) {
                val location = editTextSearch.text.toString()
                val geocoder = Geocoder(this, Locale.getDefault())

                @Suppress("DEPRECATION")    //added since getFromLocationName is deprecated
                val listAddress: List<Address>? = geocoder.getFromLocationName(location, 1)
                if (listAddress?.isNotEmpty() == true) {
                    val latLng = LatLng(listAddress[0].latitude, listAddress[0].longitude)
                    val markerOptions = MarkerOptions()
                    markerOptions.title("YOU")
                    markerOptions.position(latLng)
                    gMap.addMarker(markerOptions)
                    val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15.0F)
                    gMap.animateCamera(cameraUpdate)
                    //gMap.uiSettings.isZoomControlsEnabled = true
                } else{
                    Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
                }

            } else{
                Toast.makeText(this, "Type any location name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermission() {
        Dexter.withContext(this).withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION).withListener(object:PermissionListener {
            override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                isPermissionGranter = true

                Toast.makeText(applicationContext, "Permission Granter", Toast.LENGTH_SHORT).show()
            }

            override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts("package", packageName, "")
                intent.data = uri
                startActivity(intent)
            }

            override fun onPermissionRationaleShouldBeShown(p0: PermissionRequest?, p1: PermissionToken?) {
                isPermissionGranter = true
                p1?.continuePermissionRequest()
            }

        }).check()
    }


    //override the function in the interface
    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        gMap.uiSettings.isZoomControlsEnabled = true

        //Check whether the location permission is on
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION), 101)
            return
        }
        gMap.isMyLocationEnabled = true
        getCurrentLocationUser()
    }

    private fun openGps() {

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(500)
            .setMaxUpdateDelayMillis(1000)
            .build()

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)

        val task = LocationServices.getSettingsClient(this)
            .checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            Toast.makeText(this, "GPS on", Toast.LENGTH_SHORT).show()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException){
                try {
                    exception.startResolutionForResult(this@MainActivity,
                        0x1)
                } catch (sendEx: IntentSender.SendIntentException) {
                    sendEx.printStackTrace()
                }
            }
        }
    }

    private fun getCurrentLocationUser() {

        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location: Location? = task.result
                    if(location != null){
                        Toast.makeText(this, "Testing", Toast.LENGTH_SHORT).show()
                        val latLng = LatLng(location.latitude, location.longitude)
                        val markerOptions = MarkerOptions().position(latLng).title("YOU")

                        gMap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 7f))
                        gMap.addMarker(markerOptions)
                    }
                }
            }
        }

    }
}