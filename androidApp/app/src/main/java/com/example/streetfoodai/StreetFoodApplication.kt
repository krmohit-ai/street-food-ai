package com.example.streetfoodai

import android.app.Application
import android.preference.PreferenceManager
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class StreetFoodApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize OpenStreetMap configuration
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        
        // OSMdroid Configuration
        val osmConfig = Configuration.getInstance()
        // OSM Policy: Be honest and unique. Using your email helps avoid blocks.
        osmConfig.userAgentValue = "StreetFoodAI-Hackathon-App-prajwalshambharkar342@gmail.com"
        osmConfig.osmdroidTileCache = cacheDir
        
        osmConfig.load(this, sharedPrefs)
    }
}
