package com.example.bluetoothapp

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import android.widget.RadioButton
import android.widget.RadioGroup
import android.util.Log
import com.example.bluetoothapp.ble.BleManager
import com.example.bluetoothapp.permission.BlePermissionManager


enum class SortType {
    LAST_SEEN,
    RSSI,
    NAME
}

private var SUCCESS = true
private var FAILURE = false

class MainActivity : ComponentActivity() {

    private lateinit var bleManager: BleManager

    private lateinit var logTextView: TextView
    private lateinit var filterEditText: EditText

    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private lateinit var radioArea: RadioGroup
    private lateinit var sortButton: RadioButton
    private lateinit var rssiButton: RadioButton
    private lateinit var nameButton: RadioButton

    private var filterText = ""
    private var sortType = SortType.LAST_SEEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create UI components
        setupUi()
        // Initialize BLE manager
        if (!initializeBle()){
            // terminate Main Activity.
            finish()
            return
        }
        // Request runtime permissions
        requestPermissionsIfNeeded()
        // Register UI event listeners
        setupListeners()
    }

    private fun setupUi() {
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val buttonArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 120, 20, 20)
        }

        filterEditText = EditText(this).apply { hint = "Device Name filter" }

        startButton = Button(this).apply { text = "SCAN START" }
        stopButton = Button(this).apply { text = "SCAN STOP" }

        buttonArea.addView(filterEditText)
        buttonArea.addView(startButton)
        buttonArea.addView(stopButton)

        rootLayout.addView(buttonArea)

        radioArea = RadioGroup(this)

        sortButton = RadioButton(this).apply { text = "更新順" }
        rssiButton = RadioButton(this).apply { text = "RSSI順" }
        nameButton = RadioButton(this).apply { text = "名前順" }

        radioArea.addView(sortButton)
        radioArea.addView(rssiButton)
        radioArea.addView(nameButton)

        rootLayout.addView(radioArea)

        val scrollView = ScrollView(this)

        logTextView = TextView(this)
        scrollView.addView(logTextView)

        rootLayout.addView(scrollView)

        setContentView(rootLayout)
    }

    private fun initializeBle(): Boolean {
        if(!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Log.e("MainActivity", "BLE is not supported.")
            return FAILURE
        }
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        if(bluetoothAdapter == null){
            Log.e("MainActivity", "Bluetooth functionally is unavailable on this device.")
            return FAILURE
        }
        bleManager = BleManager(
            bluetoothAdapter = bluetoothAdapter,
            onDevicesUpdated = { updateDeviceList() }
        )
        return SUCCESS
    }

    private fun requestPermissionsIfNeeded() {
        // Request permissions at startup
        if (!BlePermissionManager.hasPermissions(this)) {
            ActivityCompat.requestPermissions(
                this,
                BlePermissionManager.requiredPermissions(),
                100
            )
        }
    }

    private fun setupListeners() {
        // Scan start button
        startButton.setOnClickListener {
            if (!BlePermissionManager.hasPermissions(this)) {
                ActivityCompat.requestPermissions(
                    this,
                    BlePermissionManager.requiredPermissions(),
                    100
                )
                return@setOnClickListener
            }
            bleManager.startScan()
        }

        // Scan stop button
        stopButton.setOnClickListener {
            bleManager.stopScan()
        }

        filterEditText.addTextChangedListener{
            filterText = it.toString()
            updateDeviceList()
        }

        radioArea.setOnCheckedChangeListener { _, checkedId ->
            sortType = when (checkedId) {
                sortButton.id -> SortType.LAST_SEEN
                rssiButton.id -> SortType.RSSI
                nameButton.id -> SortType.NAME
                else -> SortType.LAST_SEEN
            }

            updateDeviceList()
        }
    }

    private var lastUpdateTime = 0L
    private fun updateDeviceList() {
        val now = System.currentTimeMillis()
        // ディレイをかけてRSSIの更新頻度を削減
        if (now - lastUpdateTime < 500) {
            return
        }

        lastUpdateTime = now
        val devices = when (sortType) {
            SortType.LAST_SEEN -> bleManager.getDevices().sortedByDescending { it.lastSeen }
            SortType.RSSI -> bleManager.getDevices().sortedByDescending { it.rssi }
            SortType.NAME -> bleManager.getDevices().sortedBy { it.name }
        }

        runOnUiThread {
            val builder = StringBuilder()
            devices.filter{
                filterText.isBlank() || it.name.contains(filterText, ignoreCase = true)
            }.forEach { device ->
                builder.append(" NAME : ${device.name}\nRSSI : ${device.rssi}\nMAC  : ${device.address}".trimIndent())
                builder.append("\n\n")
            }
            logTextView.text = builder.toString()
        }
    }
}

