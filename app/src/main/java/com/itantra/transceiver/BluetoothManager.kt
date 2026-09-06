package com.itantra.transceiver

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID

class BluetoothManager {

    private val bluetoothAdapter: BluetoothAdapter? =
        BluetoothAdapter.getDefaultAdapter()

    private val uuid: UUID =
        UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    @SuppressLint("MissingPermission")
    fun startServer(onMessageReceived: (String) -> Unit) {

        Thread {
            try {
                val serverSocket: BluetoothServerSocket =
                    bluetoothAdapter!!.listenUsingRfcommWithServiceRecord(
                        "iTantra",
                        uuid
                    )

                socket = serverSocket.accept()

                outputStream = socket?.outputStream

                val reader = BufferedReader(
                    InputStreamReader(socket?.inputStream)
                )

                while (true) {
                    val message = reader.readLine() ?: break
                    onMessageReceived(message)
                }

                socket?.close()
                serverSocket.close()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(
        device: BluetoothDevice,
        onConnected: () -> Unit,
        onError: (String) -> Unit
    ) {

        Thread {
            try {
                socket = device.createRfcommSocketToServiceRecord(uuid)

                bluetoothAdapter?.cancelDiscovery()

                socket?.connect()

                outputStream = socket?.outputStream

                onConnected()

            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Unknown Bluetooth error")
            }
        }.start()
    }
    fun sendMessage(message: String) {

        try {
            outputStream?.write(
                "$message\n".toByteArray()
            )
            outputStream?.flush()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun disconnect() {

        try {
            socket?.close()
            socket = null
            outputStream = null

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}