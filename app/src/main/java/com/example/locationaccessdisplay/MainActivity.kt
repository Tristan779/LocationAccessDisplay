package com.example.locationaccessdisplay


import android.Manifest
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.provider.Settings
import android.telephony.TelephonyManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import com.google.android.gms.ads.identifier.AdvertisingIdClient

class MainActivity : AppCompatActivity(), LocationListener {

    private var updatesPaused = false
    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 2


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        supportActionBar?.hide()

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        findViewById<Button>(R.id.btnGetPreciseLocation).setOnClickListener {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                showToast("GPS is disabled, please enable it in settings.")
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            } else {
                getLocation(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        findViewById<Button>(R.id.btnGetCoarseLocation).setOnClickListener {
            getLocation(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        findViewById<Button>(R.id.btnPauseUpdates).setOnClickListener {
            updatesPaused = !updatesPaused // Toggle the pause state

            if (updatesPaused) {
                // Stop updates
                locationManager.removeUpdates(this)
                showToast("Location updates paused.")
            } else {
                // Optionally resume updates immediately when unpaused
                showToast("Location updates will resume on the next request.")
            }
        }

    }

    private fun getLocation(permissionType: String) {
        if (updatesPaused) return  // Don't request updates if paused

        if (ContextCompat.checkSelfPermission(this, permissionType) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permissionType), locationPermissionCode)
        } else {
            val provider = if (permissionType == Manifest.permission.ACCESS_FINE_LOCATION) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
            locationManager.requestLocationUpdates(provider, 5000, 5f, this)
            showToast("Requesting location updates.")
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation(permissions[0])
        } else {
            showToast("Location permission denied.")
        }
    }

    override fun onLocationChanged(location: Location) {
        findViewById<TextView>(R.id.tvLatitude).text = "Latitude: ${location.latitude}"
        findViewById<TextView>(R.id.tvLongitude).text = "Longitude: ${location.longitude}"
        findViewById<TextView>(R.id.tvAccuracy).text = "Accuracy: ${location.accuracy}"
        findViewById<TextView>(R.id.tvAltitude).text = "Altitude: ${location.altitude}"
        findViewById<TextView>(R.id.tvSpeed).text = "Speed: ${location.speed}"
        findViewById<TextView>(R.id.tvBearing).text = "Bearing: ${location.bearing}"
        findViewById<TextView>(R.id.tvProvider).text = "Provider: ${location.provider}"



        //network type

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        val isConnected = activeNetwork?.isConnectedOrConnecting == true
        val networkType = activeNetwork?.type

        findViewById<TextView>(R.id.tvNetworkType).text = "Network Type: ${if (isConnected) networkType.toString() else "Not Connected"}"

        //wifi state

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiState = when (wifiManager.wifiState) {
            WifiManager.WIFI_STATE_ENABLED -> "Enabled"
            WifiManager.WIFI_STATE_DISABLED -> "Disabled"
            else -> "Unknown"
        }

        findViewById<TextView>(R.id.tvWifiState).text = "Wi-Fi State: $wifiState"

        //battery status

        val batteryStatusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val batteryPct = level / scale.toFloat() * 100
                findViewById<TextView>(R.id.tvBatteryStatus).text = "Battery Status: $batteryPct%"
            }
        }
        registerReceiver(batteryStatusReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))


        //bluetooth status

        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        val isBluetoothEnabled = bluetoothAdapter?.isEnabled == true
        findViewById<TextView>(R.id.tvBluetoothStatus).text = "Bluetooth Status: ${if (isBluetoothEnabled) "Enabled" else "Disabled"}"

        //device model

        val deviceModel = Build.MODEL // Get the device model
        findViewById<TextView>(R.id.tvDeviceModel).text = "Device Model: $deviceModel"

        //brightness level
        try {
            val brightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            findViewById<TextView>(R.id.tvBrightness).text = "Brightness: $brightness"
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }

        //device manufacturer

        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        findViewById<TextView>(R.id.tvDeviceInfo).text = "Device: $manufacturer $model"

        //OS info

        val osVersion = Build.VERSION.RELEASE
        val sdkInt = Build.VERSION.SDK_INT
        findViewById<TextView>(R.id.tvOSVersion).text = "Android Version: $osVersion (SDK: $sdkInt)"


        //cpu info

        val cpuAbi = Build.CPU_ABI
        val hardware = Build.HARDWARE
        findViewById<TextView>(R.id.tvHardwareInfo).text = "CPU ABI: $cpuAbi, Hardware: $hardware"


        //display info

        val metrics = resources.displayMetrics
        val density = metrics.densityDpi
        val height = metrics.heightPixels
        val width = metrics.widthPixels
        findViewById<TextView>(R.id.tvDisplayInfo).text = "Display: ${width}x$height, Density: $density DPI"


        //storage info

        val storage = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = storage.blockSizeLong * storage.availableBlocksLong
        val totalBytes = storage.blockSizeLong * storage.blockCountLong
        findViewById<TextView>(R.id.tvStorageInfo).text = "Storage: Available ${bytesAvailable / (1024 * 1024)} MB / Total ${totalBytes / (1024 * 1024)} MB"

        //ram info

        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalRam = memoryInfo.totalMem / (1024 * 1024)
        findViewById<TextView>(R.id.tvRAMInfo).text = "RAM: Total ${totalRam}MB"

        //device identifiers
        displayDeviceIdentifiers()

        //phone on or off
        updateScreenStatus()

    }

    private fun updateScreenStatus() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn: Boolean = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
            powerManager.isInteractive
        } else {
            @Suppress("DEPRECATION")
            powerManager.isScreenOn
        }

        val screenStatusText = if (isScreenOn) "Screen is ON" else "Screen is OFF"
        findViewById<TextView>(R.id.tvPhoneState).text = screenStatusText
    }

    private fun displayDeviceIdentifiers() {
        // Display Android ID
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        findViewById<TextView>(R.id.tvAndroidID).text = "Android ID: $androidId"

        // Get Advertising ID in a coroutine as it may block the main thread
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(applicationContext)
                val adId = adInfo?.id ?: "Unavailable"
                // Switch back to the main thread to update UI
                withContext(Dispatchers.Main) {
                    findViewById<TextView>(R.id.tvAdvertisingID).text = "Advertising ID: $adId"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    findViewById<TextView>(R.id.tvAdvertisingID).text = "Advertising ID: Error retrieving"
                }
            }
        }

        findViewById<TextView>(R.id.tvGooglePlayServicesID).text = "Google Play Services ID: ExampleID"
    }

    override fun onResume() {
        super.onResume()
        updateAppForegroundStatus(true)
    }

    override fun onPause() {
        super.onPause()
        // Note: onPause is called when the activity is still partially visible, consider using onStop for full background
        updateAppForegroundStatus(false)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun updateAppForegroundStatus(isInForeground: Boolean) {
        val statusText = if (isInForeground) "App is in the foreground" else "App is not in the foreground"
        findViewById<TextView>(R.id.tvAppForegroundStatus).text = statusText
    }

    // Implement other required methods of LocationListener interface
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
