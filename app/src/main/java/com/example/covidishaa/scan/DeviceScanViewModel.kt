/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.covidishaa.scan

import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.*
import android.os.Build
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.*
import com.example.covidishaa.bluetooth.SERVICE_UUID
import com.example.covidishaa.data.ContactData
import com.example.covidishaa.data.ContactDatabase
import com.example.covidishaa.data.ContactRepository
import com.example.covidishaa.data.ContactViewModel
import com.example.covidishaa.scan.DeviceScanViewState.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*


private const val TAG = "DeviceScanViewModel"
// 30 second scan period
private const val SCAN_PERIOD = 30000L

open class DeviceScanViewModel(app: Application) : AndroidViewModel(app) {

    // LiveData for sending the view state to the DeviceScanFragment
    private val _viewState = MutableLiveData<DeviceScanViewState>()
    val viewState = _viewState as LiveData<DeviceScanViewState>

    // String key is the address of the bluetooth device
    private val scanResults = mutableMapOf<String, BluetoothDevice>()

    //initialise contact view model
//    private lateinit var mConactViewModel : ContactViewModel

    // BluetoothAdapter should never be null since BLE is required per
    // the <uses-feature> tag in the AndroidManifest.xml
    private val adapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    // This property will be null if bluetooth is not enabled
    private var scanner: BluetoothLeScanner? = null

    private var scanCallback: DeviceScanCallback? = null
    private val scanFilters: List<ScanFilter>
    private val scanSettings: ScanSettings
//    private val bytes : ByteArray? = null
//    private var userKiEmail : String? = null
//    private var key : ParcelUuid? = null


    val dao = ContactDatabase.getDatabase(app).contactDao()
    var repository =  ContactRepository(dao)




    init {
        // Setup scan filters and settings
        scanFilters = buildScanFilters()
        scanSettings = buildScanSettings()


        // Start a scan for BLE devices
        startScan()
    }

    override fun onCleared() {
        super.onCleared()
        stopScanning()
    }

    fun startScan() {
        // If advertisement is not supported on this device then other devices will not be able to
        // discover and connect to it.
        if (!adapter.isMultipleAdvertisementSupported) {
            _viewState.value = AdvertisementNotSupported
            return
        }

        if (scanCallback == null) {
            scanner = adapter.bluetoothLeScanner
            Log.d(TAG, "Start Scanning")
            // Update the UI to indicate an active scan is starting
            _viewState.value = ActiveScan

            // Stop scanning after the scan period
            Handler().postDelayed({ stopScanning() }, SCAN_PERIOD)

            // Kick off a new scan
            scanCallback = DeviceScanCallback()
            scanner?.startScan(scanFilters, scanSettings, scanCallback)
        } else {
            Log.d(TAG, "Already scanning")
        }
    }

    private fun stopScanning() {
        Log.d(TAG, "Stopping Scanning")
//        startScan()
        scanner?.stopScan(scanCallback)
        scanCallback = null
        // return the current results
        _viewState.value = ScanResults(scanResults)

        startScan()
    }

    /**
     * Return a List of [ScanFilter] objects to filter by Service UUID.
     */
    private fun buildScanFilters(): List<ScanFilter> {
        val builder = ScanFilter.Builder()
        // Comment out the below line to see all BLE devices around you
        builder.setServiceUuid(ParcelUuid(SERVICE_UUID))
        val filter = builder.build()
        return listOf(filter)
    }

    /**
     * Return a [ScanSettings] object set to use low power (to preserve battery life).
     */
    private fun buildScanSettings(): ScanSettings {
        return ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
    }

    /**
     * Custom ScanCallback object - adds found devices to list on success, displays error on failure.
     */
    private inner class DeviceScanCallback : ScanCallback() {
        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)
            for (item in results) {
                item?.let { itemkaitem ->
                    var bytes = itemkaitem.scanRecord?.serviceData?.values?.toList()?.get(0)
                    var userKiEmail = bytes?.let { String(it) }

                    if (userKiEmail == null){
                        userKiEmail = "null"
                    }

                    var sdf = SimpleDateFormat("dd/M/yyyy")
                    var currentDate = sdf.format(Date())
                    var contactdata = ContactData(currentDate,userKiEmail)
                    GlobalScope.launch(Dispatchers.IO) {
                        repository.addContact(contactdata)

                        Log.i("trycry", "added successfully")
                    }
                    scanResults[userKiEmail] = itemkaitem.device
                }
            }
            _viewState.value = ScanResults(scanResults)
        }

        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {
            super.onScanResult(callbackType, result)
            Log.i("baamzi", result.toString())
            Log.i("baamzi", "${result.scanRecord?.serviceData}")

            Log.i("baamzi", "${result.rssi}")
            result?.let { item ->
                Log.i("baamzi", item.toString())
                Log.i("baamzi", "inside")
//

                var bytes = result.scanRecord?.serviceData?.values?.toList()?.get(0)
                var userKiEmail = bytes?.let { String(it) }

                if (userKiEmail == null){
                    userKiEmail = "null"
                }
                var sdf = SimpleDateFormat("dd/M/yyyy")
                var currentDate = sdf.format(Date())
                var contactdata = ContactData(currentDate,userKiEmail)
                GlobalScope.launch(Dispatchers.IO) {
                    repository.addContact(contactdata)

                    Log.i("trycry", "added successfully")
                }
                scanResults[userKiEmail] = item.device

            }
            _viewState.value = ScanResults(scanResults)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            // Send error state to the fragment to display
            val errorMessage = "Scan failed with error: $errorCode"
            _viewState.value = Error(errorMessage)
        }
    }
}