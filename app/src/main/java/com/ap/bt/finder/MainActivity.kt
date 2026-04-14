package com.ap.bt.finder

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.ap.bt.finder.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothManager: BluetoothManagerClass
    private lateinit var audioManager: AudioManager
    private lateinit var deviceAdapter: BluetoothDeviceAdapter

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            bluetoothManager.startScan()
        } else {
            Snackbar.make(binding.root, "Permissions required for scanning", Snackbar.LENGTH_LONG).show()
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        @Suppress("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()

                    device?.let {
                        bluetoothManager.updateSignalStrength(it.address, rssi)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeComponents()
        setupRecyclerView()
        setupListeners()
        registerBluetoothReceiver()
    }

    private fun initializeComponents() {
        bluetoothManager = BluetoothManagerClass(this)
        audioManager = AudioManager(this)

        bluetoothManager.setCallback(object : BluetoothManagerCallback {
            override fun onDeviceFound(device: DiscoveredBluetoothDevice) {
                updateDeviceList()
                if (device.isConnected) {
                    audioManager.updateTrackingSignal(device.signalStrength)
                    updateTopCardUI()
                } else if (bluetoothManager.getDiscoveredDevices().none { it.isConnected }) {
                    audioManager.stopTracking()
                    updateTopCardUI()
                }
            }

            override fun onScanStarted() {
                binding.scanButton.text = "Scanning..."
                binding.scanButton.setIconResource(android.R.drawable.ic_media_pause)
            }

            override fun onScanFinished() {
                binding.scanButton.text = "Scan Devices"
                binding.scanButton.setIconResource(android.R.drawable.ic_menu_search)
            }

            override fun onSignalStrengthUpdated(address: String, strength: Int) {
                audioManager.updateTrackingSignal(strength)
                updateTopCardUI()
                updateDeviceList()
            }

            override fun onError(error: String) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            }
        })
    }

    private fun setupRecyclerView() {
        deviceAdapter = BluetoothDeviceAdapter(emptyList()) { address ->
            bluetoothManager.toggleConnection(address)
            val isNowTracking = bluetoothManager.getDiscoveredDevices().find { it.address == address }?.isConnected == true

            if (isNowTracking) {
                audioManager.startTracking()
            } else {
                audioManager.stopTracking()
            }
            updateTopCardUI()
            updateDeviceList()
        }
        binding.deviceRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = deviceAdapter
        }
    }

    private fun setupListeners() {
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }

        binding.scanButton.setOnClickListener {
            if (bluetoothManager.isScanning()) {
                bluetoothManager.stopScan()
            } else {
                if (PermissionManager.hasBluetoothPermissions(this)) {
                    bluetoothManager.startScan()
                } else {
                    permissionLauncher.launch(PermissionManager.getRequiredPermissions())
                }
            }
        }

        binding.clearButton.setOnClickListener {
            deviceAdapter.clearDevices()
            bluetoothManager.stopScan()
            audioManager.stopTracking()
            binding.topSignalCard.visibility = View.GONE
        }
    }

    private fun updateTopCardUI() {
        val activeDevice = bluetoothManager.getDiscoveredDevices().find { it.isConnected }

        if (activeDevice == null) {
            binding.topSignalCard.visibility = View.GONE
            return
        }

        binding.topSignalCard.visibility = View.VISIBLE
        binding.connectedDeviceName.text = activeDevice.name

        val rssi = activeDevice.signalStrength
        if (rssi == Constants.SIGNAL_NOT_FOUND) {
            binding.connectedDeviceStatus.text = "Waiting for signal..."
            binding.signalProgressBar.progress = 0
            binding.signalValueText.text = "..."
        } else {
            binding.connectedDeviceStatus.text = "Tracking Live Signal"
            val minDbm = Constants.MIN_SIGNAL_DBM
            val maxDbm = Constants.MAX_SIGNAL_DBM
            val normalized = (((rssi - minDbm).toFloat() / (maxDbm - minDbm)) * 100).toInt().coerceIn(0, 100)

            binding.signalProgressBar.progress = normalized
            binding.signalValueText.text = "$rssi dBm ($normalized%)"

            val color = when {
                rssi >= Constants.SIGNAL_THRESHOLD_STRONG -> ContextCompat.getColor(this, R.color.signal_strong)
                rssi >= Constants.SIGNAL_THRESHOLD_MEDIUM -> ContextCompat.getColor(this, R.color.signal_medium)
                else -> ContextCompat.getColor(this, R.color.signal_weak)
            }
            binding.signalProgressBar.setIndicatorColor(color)
        }
    }

    private fun updateDeviceList() {
        val sortedDevices = bluetoothManager.getDiscoveredDevices().sortedWith(
            compareByDescending<DiscoveredBluetoothDevice> { it.isConnected }
                .thenByDescending { it.signalStrength }
        )
        deviceAdapter.updateDevices(sortedDevices)
    }

    private fun registerBluetoothReceiver() {
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(bluetoothReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
        bluetoothManager.destroy()
        audioManager.release()
    }
}