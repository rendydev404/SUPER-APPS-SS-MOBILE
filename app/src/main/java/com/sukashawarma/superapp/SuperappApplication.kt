package com.sukashawarma.superapp

import android.app.Application
import com.sukashawarma.superapp.data.local.AuthPrefs
import com.sukashawarma.superapp.data.remote.NetworkMonitor
import com.sukashawarma.superapp.data.remote.SupabaseClient
import com.sukashawarma.superapp.data.remote.AuthSessionManager
import com.sukashawarma.superapp.data.location.LocationTracking
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.absensi.notif.AbsenReminder
import com.sukashawarma.superapp.feature.distribusi.data.VerifikasiDraftStore
import com.sukashawarma.superapp.notif.FcmTokenRegistrar
import com.sukashawarma.superapp.notif.SuperappMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class SuperappApplication : Application() {

    private val lingkup = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        AuthPrefs.init(this)
        VerifikasiDraftStore.init(this)
        SupabaseClient.onRefreshNeeded = { AuthSessionManager.refresh() }
        NetworkMonitor.init(this)
        // Sesi habis / logout: pelacakan lokasi kehilangan `outlet_staff_id` tujuannya,
        // jadi harus mati bersama sesi, bukan menunggu user mematikannya manual.
        AppSession.onSignOut = { LocationTracking.stop(this) }
        // Menjadwalkan ulang tiap app dibuka. Alarm yang sudah ada ditimpa lewat
        // FLAG_UPDATE_CURRENT, jadi ini sekaligus jaring pengaman kalau OEM
        // membersihkan alarm proses yang lama tidak dipakai.
        AbsenReminder.schedule(this)

        SuperappMessagingService.siapkanSaluran(this)
        ikatDeviceKeSesi()
    }

    /**
     * Mendaftarkan device ke akun yang sedang login, setiap kali sesi berganti.
     *
     * Diamati dari sini, bukan dipanggil di dalam [AppSession]: modul `core:roles`
     * sengaja tidak mengenal Firebase maupun modul lain yang bergantung padanya —
     * pola yang sama dipakai `onSignOut`.
     *
     * `distinctUntilChangedBy { it.id }` menahan pendaftaran ulang saat profil
     * staf yang sama dimuat ulang; yang perlu dikirim hanya pergantian akun.
     */
    private fun ikatDeviceKeSesi() {
        lingkup.launch {
            AppSession.staff
                .filterNotNull()
                .distinctUntilChangedBy { it.id }
                .collect { staf ->
                    FcmTokenRegistrar.daftarkan(applicationContext, staf.id, staf.outletId)
                }
        }
    }
}
