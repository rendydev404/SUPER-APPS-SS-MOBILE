package com.sukashawarma.superapp

import android.app.Application
import com.sukashawarma.superapp.data.local.AuthPrefs
import com.sukashawarma.superapp.data.remote.NetworkMonitor
import com.sukashawarma.superapp.data.remote.SupabaseClient
import com.sukashawarma.superapp.data.remote.AuthSessionManager
import com.sukashawarma.superapp.data.location.LocationTracking
import com.sukashawarma.superapp.domain.session.AppSession
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SuperappApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AuthPrefs.init(this)
        SupabaseClient.onRefreshNeeded = { AuthSessionManager.refresh() }
        NetworkMonitor.init(this)
        // Sesi habis / logout: pelacakan lokasi kehilangan `outlet_staff_id` tujuannya,
        // jadi harus mati bersama sesi, bukan menunggu user mematikannya manual.
        AppSession.onSignOut = { LocationTracking.stop(this) }
    }
}
