package com.example.bluetoothapp

import android.Manifest
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.widget.addTextChangedListener
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import android.widget.RadioButton
import android.widget.RadioGroup
import android.util.Log
import com.example.bluetoothapp.ble.BleManager
import com.example.bluetoothapp.permission.BlePermissionManager
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.ComposeView
import com.example.bluetoothapp.ble.BleDevice
import com.example.bluetoothapp.ui.DeviceList
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import com.example.bluetoothapp.ble.ServiceInfo
import com.example.bluetoothapp.ui.NotifyLogBottomSheet
import com.example.bluetoothapp.ui.ServiceBottomSheet
import androidx.activity.OnBackPressedCallback
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import com.example.bluetoothapp.service.BleService

enum class SortType {
    LAST_SEEN,
    RSSI,
    NAME
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private lateinit var bleManager: BleManager

    private lateinit var filterEditText: EditText

    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private lateinit var radioArea: RadioGroup
    private lateinit var sortButton: RadioButton
    private lateinit var rssiButton: RadioButton
    private lateinit var nameButton: RadioButton

    private var filterText = ""
    private var sortType = SortType.LAST_SEEN
    private lateinit var composeView: ComposeView
    private val isBleReady = mutableStateOf(false)
    private val visibleDevices = mutableStateListOf<BleDevice>()
    private val showServiceSheet = mutableStateOf(false)
    private val showLogSheet = mutableStateOf(false)
    private val selectedDevice = mutableStateOf<BleDevice?>(null)

    private var bleService: BleService? = null
    private var isBound = false

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate called.")

        // Register BackHandler
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d("LIFECYCLE", "BackPressed handled. Cleaning up resources.")
                cleanupAndFinish()
            }
        })

        // Create UI components
        setupUi()
        // Request runtime permissions
        requestPermissionsIfNeeded()
    }

    override fun onStart() {
        Log.d("MainActivity", "onStart called.")
        super.onStart()
        val intent = Intent(this, BleService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onPause() {
        super.onPause()
        if (!isBleReady.value) return
    }

    override fun onResume() {
        super.onResume()
        if (!isBleReady.value) return
        updateDeviceList()
    }

    override fun onStop() {
        super.onStop()
        if (!isBleReady.value) return
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("LIFECYCLE", "onDestroy called")

        isBleReady.value = false
        unbindService()
    }

    private fun cleanupAndFinish() {
        stopService(Intent(this, BleService::class.java))
        finish()
    }

    private fun unbindService() {
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    private val serviceConnection = object : android.content.ServiceConnection {
        override fun onServiceConnected(name: android.content.ComponentName, service: android.os.IBinder) {
            val binder = service as BleService.LocalBinder
            bleService = binder.getService()
            bleManager = bleService!!.getBleManager()
            isBound = true
            Log.d("MainActivity", "BleService connected, BleManager obtained")

            bleManager.setOnDevicesUpdated { updateDeviceList() }
            setupListeners()
            isBleReady.value = true
            updateDeviceList()
        }

        override fun onServiceDisconnected(name: android.content.ComponentName) {
            isBound = false
            isBleReady.value = false
            Log.d("MainActivity", "BleService disconnected")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
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

        composeView = ComposeView(this)

        rootLayout.addView(
            composeView,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        setContentView(rootLayout)
        composeView.setContent {
            if (!isBleReady.value) {
                Text(text = "Connecting BRE Service...")
                return@setContent
            }
            val serviceList = remember { mutableStateListOf<ServiceInfo>()}

            DeviceList(
                devices = visibleDevices,
                onConnect = { device -> bleManager.connect(device) },
                onDisconnect = { device -> bleManager.disconnect(device) },
                onShowServices = { device ->
                    serviceList.clear()
                    serviceList.addAll( bleManager.getDiscoveredServices())
                    selectedDevice.value = device
                    showServiceSheet.value = true
                },
                onShowLogs = { showLogSheet.value = true }
            )
            if (showServiceSheet.value) {
                ServiceBottomSheet(
                    services = serviceList,
                    onDismiss = { showServiceSheet.value = false },
                    onReadRequest = { characteristicInfo ->
                        bleManager.readCharacteristic(characteristicInfo.characteristic)
                    },
                    onWriteRequest = { characteristicInfo, payload ->
                        bleManager.writeCharacteristic(
                            characteristicInfo.characteristic,
                            payload.toByteArray(Charsets.UTF_8)
                        )
                    },
                    onNotifyRequest = { characteristicInfo ->
                        bleManager.enableNotify(characteristicInfo.characteristic)
                    }
                )
            }
            if (showLogSheet.value) {
                NotifyLogBottomSheet(onDismiss = { showLogSheet.value = false })
            }
        }
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
            Log.d(
                "Permission",
                BlePermissionManager.hasPermissions(this).toString()
            )
            bleManager.startScan()
        }

        // Scan stop button
        stopButton.setOnClickListener {
            bleManager.stopScan()
        }

        filterEditText.addTextChangedListener{
            filterText = it.toString()
            Log.d(
                "FILTER",
                "keyword=$filterText"
            )
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
            visibleDevices.clear()
            val filteredDevices = devices.filter {
                filterText.isBlank() || it.name.contains(filterText, ignoreCase = true)
            }
            visibleDevices.addAll(    filteredDevices.map { it.copy() }
            )
        }
    }
}

