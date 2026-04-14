package com.ap.bt.finder

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.*

class AudioManager(context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private var soundPool: SoundPool? = null
    private var beepSoundId: Int = 0

    private var trackingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var currentSignal = Constants.SIGNAL_NOT_FOUND

    init {
        initSoundPool()
    }

    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        // Note: In a real app, you should load a sound file here.
        // Example: beepSoundId = soundPool?.load(context, R.raw.beep, 1) ?: 0
    }

    /**
     * Updates the signal strength for the continuous tracking loop
     */
    fun updateTrackingSignal(signalStrength: Int) {
        currentSignal = signalStrength
    }

    /**
     * Starts the dynamic radar-like tracking loop.
     * Speed, volume, and vibration intensity increase as signal gets stronger.
     */
    fun startTracking() {
        if (trackingJob?.isActive == true) return

        trackingJob = scope.launch {
            while (isActive) {
                if (currentSignal != Constants.SIGNAL_NOT_FOUND) {
                    val normalized = getNormalizedSignalStrength(currentSignal)

                    if (normalized > 0) {
                        playDynamicBeep(normalized)
                        vibrateDynamic(normalized)
                    }

                    // Delay calculation: 100% signal -> ~200ms delay (fast) | 0% signal -> 1500ms delay (slow)
                    val delayMs = 1500L - (normalized * 13L)
                    delay(delayMs.coerceIn(200L, 1500L))
                } else {
                    delay(1000L) // Wait if no signal
                }
            }
        }
    }

    /**
     * Stops the tracking loop and cancels all vibrations.
     */
    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        currentSignal = Constants.SIGNAL_NOT_FOUND
        vibrator.cancel()
    }

    private fun playDynamicBeep(normalizedStrength: Int) {
        // Volume scales from 0.1 to 1.0 based on signal
        val volume = (normalizedStrength / 100f).coerceIn(0.1f, 1.0f)
        // Pitch scales slightly as you get closer
        val pitch = 1.0f + (normalizedStrength / 200f)

        soundPool?.play(beepSoundId, volume, volume, 1, 0, pitch)
    }

    private fun vibrateDynamic(normalizedStrength: Int) {
        if (!vibrator.hasVibrator()) return

        // Duration scales with strength: 50ms to 150ms
        val duration = 50L + (normalizedStrength.toLong())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Amplitude scales with strength: 50 to 255 (max)
            val amplitude = 50 + (normalizedStrength * 2)
            val effect = VibrationEffect.createOneShot(duration, amplitude.coerceIn(50, 255))
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    private fun getNormalizedSignalStrength(signalStrength: Int): Int {
        val min = Constants.MIN_SIGNAL_DBM.toFloat()
        val max = Constants.MAX_SIGNAL_DBM.toFloat()
        return (((signalStrength - min) / (max - min)) * 100).toInt().coerceIn(0, 100)
    }

    fun release() {
        stopTracking()
        scope.cancel()
        soundPool?.release()
        soundPool = null
    }
}