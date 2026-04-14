// Constants.kt
// Location: app/src/main/java/com/ap/bt/finder/Constants.kt

package com.ap.bt.finder

object Constants {
    // Developer Info
    const val DEVELOPER_NAME = "AP"
    const val DEVELOPER_EMAIL = "ap0803apap@gmail.com"
    const val GITHUB_URL = "https://github.com/ap0803apap-sketch"

    // Bluetooth Calibration
    const val SIGNAL_NOT_FOUND = -999
    const val MIN_SIGNAL_DBM = -100
    const val MAX_SIGNAL_DBM = -40
    const val SIGNAL_THRESHOLD_STRONG = -60
    const val SIGNAL_THRESHOLD_MEDIUM = -80

    // Vibration Patterns
    val VIBRATION_WEAK = longArrayOf(100)
    val VIBRATION_MEDIUM = longArrayOf(150)
    val VIBRATION_STRONG = longArrayOf(200)

    // Beep Frequencies (Hz)
    const val BEEP_FREQUENCY_WEAK = 400f
    const val BEEP_FREQUENCY_MEDIUM = 800f
    const val BEEP_FREQUENCY_STRONG = 1200f

    // Shared Preferences
    const val PREFS_NAME = "bt_finder_prefs"
    const val PREF_THEME = "theme_mode"
    const val PREF_AMOLED = "amoled_mode"
    const val PREF_DYNAMIC_COLOR = "dynamic_color"

    // Theme Modes
    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2
}