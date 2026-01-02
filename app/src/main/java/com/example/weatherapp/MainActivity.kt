package com.example.weatherapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// --- 1. DATA MODELS (To hold the API data) ---
data class WeatherResponse(val main: Main, val weather: List<Weather>)
data class Main(val temp: Float)
data class Weather(val icon: String, val description: String)

// --- 2. API INTERFACE ---
interface WeatherApi {
    @GET("data/2.5/weather")
    fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric", // Use "imperial" for Fahrenheit
        @Query("appid") apiKey: String
    ): Call<WeatherResponse>
}

class MainActivity : AppCompatActivity() {

    // REPLACE WITH YOUR OPENWEATHERMAP API KEY
    private val API_KEY = ""

    private lateinit var tvTemp: TextView
    private lateinit var ivIcon: ImageView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Views
        tvTemp = findViewById(R.id.tvTemperature)
        ivIcon = findViewById(R.id.ivWeatherIcon)

        // Initialize Location Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Check Permissions and Get Weather
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request Permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        } else {
            // Permission already granted, get location
            getLocation()
        }
    }

    // Handle Permission Result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation()
        } else {
            Toast.makeText(this, "Permission denied. Cannot show weather.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getLocation() {
        // Simple way to get the last known location
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    fetchWeather(location.latitude, location.longitude)
                } else {
                    // Note: On an emulator, 'lastLocation' might be null initially.
                    // Set a location in the emulator extended controls if this happens.
                    tvTemp.text = "Location null"
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun fetchWeather(lat: Double, lon: Double) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(WeatherApi::class.java)
        val call = api.getCurrentWeather(lat, lon, "metric", API_KEY)

        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    val weatherData = response.body()
                    if (weatherData != null) {
                        // Update UI
                        val temp = weatherData.main.temp
                        val iconCode = weatherData.weather[0].icon
                        val iconUrl = "https://openweathermap.org/img/wn/$iconCode@4x.png"

                        tvTemp.text = String.format("%.1f°C", temp)

                        // Load Icon using Glide
                        Glide.with(this@MainActivity)
                            .load(iconUrl)
                            .into(ivIcon)
                    }
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                tvTemp.text = "Error"
            }
        })
    }
}