🔍 Bluetooth Device Finder

A modern, highly responsive Android application designed to help you locate lost Bluetooth devices (headphones, smartwatches, speakers, etc.) using real-time signal strength (RSSI) tracking.

Built entirely with Kotlin, this app features a sleek Material 3 Expressive design, dynamic colors, and a radar-like audio/haptic feedback system that guides you right to your missing device.

✨ Features

Dual Bluetooth Scanning: Scans for both Classic Bluetooth and Bluetooth Low Energy (BLE) devices simultaneously.

🎯 Radar-Like Tracking: Acts like a metal detector for your devices. As you get closer (signal strength increases), the audio beeps and haptic vibrations dynamically increase in speed and intensity.

📊 Precise Signal Smoothing: Uses a moving-average filter to stabilize RSSI jumps, providing a smooth and accurate signal percentage UI.

🎨 Material 3 Expressive UI: Fully adopts Google's latest Material Design 3 guidelines with expressive switches, rounded dialogs, and elevated cards.

🖌️ Dynamic Colors (Material You): Extracts colors from your device's wallpaper to theme the app uniquely for you.

🌙 True AMOLED Dark Mode: Includes a dedicated pure black dark mode to save battery on OLED screens, alongside standard Light and Dark modes.

⚡ Instant Theme Switching: Themes and colors apply instantly across the entire app without requiring a manual app restart.

📱 Screenshots

(Add your screenshots here by replacing the placeholder links)

<img src="link_to_main_screen.png" width="250">

<img src="link_to_tracking_screen.png" width="250">

<img src="link_to_settings_screen.png" width="250">

Main Scanner

Active Tracking

Settings (Material 3)

🛠️ Tech Stack & Architecture

Language: Kotlin

UI Framework: Android View System (XML) with Material Components for Android

Asynchrony: Kotlin Coroutines (Used for the dynamic audio/vibration tracking loop)

Architecture: Activity/View-binding based with centralized Managers (BluetoothManagerClass, AudioManager).

Theming: Dynamic Colors, DayNight AppCompat Delegate.

📋 Requirements

Minimum SDK: API 31 (Android 12)

Target SDK: API 35 (Android 15)

Hardware: Device with Bluetooth and Location capabilities.

🚀 Getting Started

Prerequisites

Android Studio (Latest stable version recommended)

An Android physical device running Android 12 or higher (Bluetooth scanning cannot be properly tested on an emulator).


Open the project in Android Studio.

Sync the project with Gradle files.

Build and run the app on your physical device.

Note: The app will request Location and Nearby Devices (Bluetooth) permissions on the first launch, which are required by the Android OS to scan for BLE devices.

🤝 Contributing

Contributions, issues, and feature requests are welcome!
Feel free to check the issues page if you want to contribute.


Developed by AP Developer
