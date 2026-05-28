package com.example.kursovaya.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

class NetworkConnectivityMonitor(
    context: Context,
    private val onConnectivityChanged: (Boolean) -> Unit,
) {
    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val callback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                postCurrentState()
            }

            override fun onLost(network: Network) {
                postCurrentState()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                postCurrentState()
            }
        }

    private fun postCurrentState() {
        onConnectivityChanged(isNetworkAvailable())
    }

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private var registered = false

    fun register() {
        if (registered) return
        connectivityManager.registerDefaultNetworkCallback(callback)
        registered = true
        postCurrentState()
    }

    fun unregister() {
        if (!registered) return
        try {
            connectivityManager.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
        }
        registered = false
    }
}
