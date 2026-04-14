package com.ap.bt.finder

import android.app.Activity
import android.app.Application
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors

class BtFinderApplication : Application() {

    // Track the UI preference state per activity to apply changes instantly on resume
    private val activityThemeHashes = mutableMapOf<Activity, Int>()

    // Keep a strong reference to the listener to prevent it from being Garbage Collected
    private lateinit var prefListener: SharedPreferences.OnSharedPreferenceChangeListener

    override fun onCreate() {
        super.onCreate()

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        fun getUiStateHash(): Int {
            val theme = prefs.getString("theme_mode", "0")
            val amoled = prefs.getBoolean("amoled_mode", false)
            val dynamic = prefs.getBoolean("dynamic_color", true)
            return listOf(theme, amoled, dynamic).hashCode()
        }

        // 1. Helper to apply global App Theme (Light/Dark/System)
        fun applyThemeMode(themeString: String?) {
            when (themeString?.toIntOrNull() ?: 0) {
                Constants.THEME_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                Constants.THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                Constants.THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }

        // Apply immediately on startup
        applyThemeMode(prefs.getString("theme_mode", "0"))

        // 2. Listen for Light/Dark mode changes to apply globally instantly
        prefListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "theme_mode") {
                applyThemeMode(sharedPreferences.getString(key, "0"))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(prefListener)

        // 3. Apply Dynamic Colors and AMOLED theme globally to every Activity
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {

                // Check and apply AMOLED theme if conditions are met
                val isAmoled = prefs.getBoolean("amoled_mode", false)
                val isNightMode = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                if (isAmoled && isNightMode) {
                    activity.setTheme(R.style.Theme_BTFinder_Amoled)
                }

                // Check and apply Dynamic Colors if enabled
                val useDynamicColors = prefs.getBoolean("dynamic_color", true)
                if (useDynamicColors) {
                    DynamicColors.applyToActivityIfAvailable(activity)
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                // Record the UI state hash when this activity was created
                activityThemeHashes[activity] = getUiStateHash()
            }

            override fun onActivityResumed(activity: Activity) {
                // If UI settings changed while this activity was in the background, recreate it instantly!
                val currentHash = getUiStateHash()
                if (activityThemeHashes[activity] != currentHash) {
                    activity.recreate()
                }
            }

            override fun onActivityDestroyed(activity: Activity) {
                // Prevent memory leaks
                activityThemeHashes.remove(activity)
            }

            // Unused overrides
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        })
    }
}