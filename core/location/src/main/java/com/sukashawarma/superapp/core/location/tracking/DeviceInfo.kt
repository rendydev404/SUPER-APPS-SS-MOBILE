package com.sukashawarma.superapp.data.location

import android.content.Context
import android.os.Build
import android.provider.Settings

object DeviceInfo {

    /**
     * Nama HP yang dikirim bersama posisi. Nama buatan user (yang dipakai Bluetooth/hotspot)
     * jauh lebih berguna untuk SPV daripada kode model mentah — "HP Outlet Sukmajaya" vs
     * "SM-A075F". Model tetap diikutkan sebagai penanda pasti kalau namanya generik atau
     * dua HP diberi nama sama.
     */
    fun name(context: Context): String {
        val model = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}".trim()
        val custom = try {
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)?.trim()
        } catch (e: Exception) {
            null
        }
        return if (custom.isNullOrBlank() || custom.equals(model, ignoreCase = true) ||
            custom.equals(Build.MODEL, ignoreCase = true)
        ) {
            model
        } else {
            "$custom ($model)"
        }
    }
}
