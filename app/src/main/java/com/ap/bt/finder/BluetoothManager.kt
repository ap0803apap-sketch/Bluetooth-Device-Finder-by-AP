package com.ap.bt.finder

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission

data class DiscoveredBluetoothDevice(
    val name: String,
    val address: String,
    val signalStrength: Int,
    val paired: Boolean,
    val isConnected: Boolean = false // Represents if the device is currently being tracked (Playing)
)

interface BluetoothManagerCallback {
    fun onDeviceFound(device: DiscoveredBluetoothDevice)
    fun onScanStarted()
    fun onScanFinished()
    fun onSignalStrengthUpdated(address: String, strength: Int)
    fun onError(error: String)
}

class BluetoothManagerClass(context: Context) {

    private val context = context.applicationContext
    private val bluetoothAdapter: BluetoothAdapter?
    private val bluetoothLeScanner by lazy { bluetoothAdapter?.bluetoothLeScanner }
    private var isScanning = false
    private val discoveredDevices = mutableMapOf<String, DiscoveredBluetoothDevice>()
    private var callback: BluetoothManagerCallback? = null

    // Moving average filter to stabilize RSSI jumps (makes signal UI much smoother)
    private val rssiHistory = mutableMapOf<String, MutableList<Int>>()

    private val leScanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            @Suppress("MissingPermission")

            val smoothedRssi = getSmoothedRssi(device.address, result.rssi)

            val btDevice = DiscoveredBluetoothDevice(
                name = device.name ?: "Unknown Device",
                address = device.address,
                signalStrength = smoothedRssi,
                paired = device.bondState == BluetoothDevice.BOND_BONDED
            )
            addDiscoveredDevice(btDevice)
        }

        override fun onScanFailed(errorCode: Int) {
            callback?.onError("BLE Scan Failed: $errorCode")
        }
    }

    companion object {
        private const val TAG = "BluetoothManager"
    }

    init {
        val bluetoothManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(BluetoothManager::class.java)
        } else {
            context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        }
        bluetoothAdapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
    }

    fun setCallback(callback: BluetoothManagerCallback) {
        this.callback = callback
    }

    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled ?: false

    private fun getSmoothedRssi(address: String, newRssi: Int): Int {
        val history = rssiHistory.getOrPut(address) { mutableListOf() }
        history.add(newRssi)
        // Keep the last 5 readings for a stable average
        if (history.size > 5) history.removeAt(0)
        return history.average().toInt()
    }

    @Suppress("MissingPermission")
    fun startScan() {
        if (isScanning) return
        if (!isBluetoothEnabled()) { callback?.onError("Bluetooth is not enabled"); return }

        isScanning = true
        rssiHistory.clear()

        try {
            // Start Classic Discovery
            bluetoothAdapter?.startDiscovery()
            // Start BLE Scan
            val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
            bluetoothLeScanner?.startScan(null, settings, leScanCallback)
            callback?.onScanStarted()
        } catch (e: Exception) {
            callback?.onError("Error starting scan: ${e.message}")
            stopScan()
        }
    }

    @Suppress("MissingPermission")
    fun stopScan() {
        if (!isScanning) return
        isScanning = false
        try {
            bluetoothAdapter?.cancelDiscovery()
            bluetoothLeScanner?.stopScan(leScanCallback)
            callback?.onScanFinished()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan", e)
        }
    }

    fun setScanningState(scanning: Boolean) {
        this.isScanning = scanning
        if (!scanning) callback?.onScanFinished()
    }

    fun getDiscoveredDevices(): List<DiscoveredBluetoothDevice> = discoveredDevices.values.toList()

    fun addDiscoveredDevice(device: DiscoveredBluetoothDevice) {
        val existing = discoveredDevices[device.address]

        val updatedDevice = if (existing != null) {
            existing.copy(
                name = if (existing.name == "Unknown Device") device.name else existing.name,
                signalStrength = device.signalStrength,
                paired = existing.paired || device.paired
            )
        } else {
            device
        }

        discoveredDevices[device.address] = updatedDevice
        callback?.onDeviceFound(updatedDevice)

        // Notify signal update if this is the actively tracked device
        if (updatedDevice.isConnected) {
            callback?.onSignalStrengthUpdated(updatedDevice.address, updatedDevice.signalStrength)
        }
    }

    fun toggleConnection(address: String) {
        val targetDevice = discoveredDevices[address] ?: return
        val willBeConnected = !targetDevice.isConnected

        // Ensures ONLY ONE device can be tracked at a time.
        discoveredDevices.keys.forEach { key ->
            val d = discoveredDevices[key]!!
            discoveredDevices[key] = d.copy(isConnected = if (key == address) willBeConnected else false)
        }

        callback?.onDeviceFound(discoveredDevices[address]!!)
    }

    fun updateSignalStrength(address: String, strength: Int) {
        val smoothed = getSmoothedRssi(address, strength)
        discoveredDevices[address]?.let {
            discoveredDevices[address] = it.copy(signalStrength = smoothed)
            if (it.isConnected) {
                callback?.onSignalStrengthUpdated(address, smoothed)
            }
        }
    }

    fun isScanning(): Boolean = isScanning

    fun destroy() { stopScan() }
}