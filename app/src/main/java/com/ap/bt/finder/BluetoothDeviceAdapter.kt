package com.ap.bt.finder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ap.bt.finder.databinding.ItemDeviceBinding

class BluetoothDeviceAdapter(
    private var devices: List<DiscoveredBluetoothDevice>,
    private val onConnectClick: (String) -> Unit
) : RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceViewHolder>() {

    inner class DeviceViewHolder(private val binding: ItemDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(device: DiscoveredBluetoothDevice) {
            with(binding) {
                deviceName.text = device.name
                deviceAddress.text = device.address
                val rssi = device.signalStrength

                // Dynamic Play/Pause Button Logic
                if (device.isConnected) {
                    playPauseButton.setIconResource(android.R.drawable.ic_media_pause)
                    playPauseButton.text = "Tracking"
                    root.strokeWidth = 4 // Highlight active card
                    root.strokeColor = ContextCompat.getColor(root.context, R.color.primary)
                } else {
                    playPauseButton.setIconResource(android.R.drawable.ic_media_play)
                    playPauseButton.text = "Track"
                    root.strokeWidth = 0 // Remove highlight
                }

                playPauseButton.setOnClickListener {
                    onConnectClick(device.address)
                }

                if (rssi == Constants.SIGNAL_NOT_FOUND) {
                    signalStrength.progress = 0
                    signalValue.text = "Searching..."
                } else {
                    val normalized = (((rssi - Constants.MIN_SIGNAL_DBM).toFloat() /
                            (Constants.MAX_SIGNAL_DBM - Constants.MIN_SIGNAL_DBM)) * 100).toInt().coerceIn(0, 100)

                    signalStrength.progress = normalized
                    signalValue.text = "${rssi} dBm (${normalized}%)"

                    val color = when {
                        rssi >= Constants.SIGNAL_THRESHOLD_STRONG -> ContextCompat.getColor(root.context, R.color.signal_strong)
                        rssi >= Constants.SIGNAL_THRESHOLD_MEDIUM -> ContextCompat.getColor(root.context, R.color.signal_medium)
                        else -> ContextCompat.getColor(root.context, R.color.signal_weak)
                    }
                    signalStrength.setIndicatorColor(color)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int = devices.size

    fun updateDevices(newDevices: List<DiscoveredBluetoothDevice>) {
        devices = newDevices
        notifyDataSetChanged()
    }

    fun clearDevices() {
        devices = emptyList()
        notifyDataSetChanged()
    }
}