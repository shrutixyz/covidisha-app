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

import android.bluetooth.BluetoothDevice
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.covidishaa.R
import java.text.SimpleDateFormat
import java.util.*

class DeviceScanViewHolder(
    view: View,
    val onDeviceSelected: (BluetoothDevice) -> Unit
) : RecyclerView.ViewHolder(view), View.OnClickListener {

    private val name = itemView.findViewById<TextView>(R.id.device_name)
    private val address = itemView.findViewById<TextView>(R.id.device_address)
    private val userEmail = itemView.findViewById<TextView>(R.id.userKiEmail)
    private val time = itemView.findViewById<TextView>(R.id.time)
    private var bluetoothDevice: BluetoothDevice? = null

    init {
        itemView.setOnClickListener(this)
    }
//    {"aakzsh": {}}
    fun bind(d : Map<String,BluetoothDevice>) {
        Log.i("bindzi", d.toString())
        var key = d.keys.toList()[0]
        var device = d.values.toList()[0]
        val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
        val currentDate = sdf.format(Date())

        bluetoothDevice = device


        name.text = device.name
        address.text = device.address
        userEmail.text = key
        time.text = currentDate


    }


    override fun onClick(view: View) {
        bluetoothDevice?.let { device ->
//            onDeviceSelected(device)
        }
    }
}