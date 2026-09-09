package com.sukashawarma.superapp.feature.stok.ui.area

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.feature.stok.data.*
import com.sukashawarma.superapp.feature.stok.domain.stokErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HargaState(val loading: Boolean = true, val error: String? = null, val days: Int? = 30,
    val items: List<MaterialPrice> = emptyList(), val detail: MaterialPrice? = null,
    val history: List<PricePurchase> = emptyList(), val historyLoading: Boolean = false, val historyError: String? = null)

class HargaBahanViewModel : ViewModel() {
    private val mutable = MutableStateFlow(HargaState())
    val state = mutable.asStateFlow()
    private var job: Job? = null
    private var historyJob: Job? = null
    init { refresh() }
    fun refresh(days: Int? = mutable.value.days) {
        job?.cancel()
        job = viewModelScope.launch {
            mutable.update { it.copy(loading = true, error = null, days = days) }
            try { val rows = HargaBahanRepository.load(days); mutable.update { it.copy(items = rows, loading = false) } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(loading = false, error = stokErrorMessage(e)) } }
        }
    }
    fun detail(item: MaterialPrice?) {
        historyJob?.cancel()
        mutable.update { it.copy(detail = item, history = emptyList(), historyError = null, historyLoading = item != null) }
        if (item == null) return
        historyJob = viewModelScope.launch {
            try { val rows = HargaBahanRepository.purchases(item.id); mutable.update { it.copy(history = rows, historyLoading = false) } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(historyLoading = false, historyError = stokErrorMessage(e)) } }
        }
    }
}

data class WasteState(val loading: Boolean = true, val reports: List<WasteReview> = emptyList(),
    val error: String? = null, val message: String? = null, val busy: Boolean = false,
    val confirmation: WasteReview? = null, val rejecting: WasteReview? = null)

class WasteApprovalViewModel : ViewModel() {
    private val mutable = MutableStateFlow(WasteState())
    val state = mutable.asStateFlow()
    private var job: Job? = null
    init { refresh() }
    fun refresh() {
        if (mutable.value.busy || job?.isActive == true) return
        job = viewModelScope.launch { load() }
    }
    private suspend fun load() {
        mutable.update { it.copy(loading = it.reports.isEmpty(), error = null) }
        try { val rows = WasteApprovalRepository.load(); mutable.update { it.copy(loading = false, reports = rows) } }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { mutable.update { it.copy(loading = false, error = stokErrorMessage(e)) } }
    }
    fun clearMessage() { mutable.update { it.copy(message = null, error = null) } }
    fun dismiss() { if (!mutable.value.busy) mutable.update { it.copy(confirmation = null, rejecting = null) } }
    fun reject(report: WasteReview) { if (!mutable.value.busy) mutable.update { it.copy(rejecting = report) } }
    fun approve(report: WasteReview) {
        if (mutable.value.busy) return
        job?.cancel()
        // Recheck stock immediately before approval; the list may have been open for a while.
        viewModelScope.launch {
            mutable.update { it.copy(busy = true, error = null) }
            try {
                val fresh = WasteApprovalRepository.reload(report)
                    ?: error("Laporan sudah diproses. Silakan muat ulang.")
                if (fresh.deficit) mutable.update { it.copy(confirmation = fresh) }
                else { WasteApprovalRepository.decide(fresh); selesai(fresh, "Laporan disetujui") }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(error = e.message ?: stokErrorMessage(e)) } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
    fun decide(report: WasteReview, reason: String? = null) {
        if (mutable.value.busy || (reason != null && reason.isBlank())) return
        job?.cancel()
        viewModelScope.launch {
            mutable.update { it.copy(busy = true, error = null) }
            try {
                WasteApprovalRepository.decide(report, reason)
                selesai(report, if (reason == null) "Laporan disetujui" else "Laporan ditolak")
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(error = e.message ?: stokErrorMessage(e)) } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }

    /**
     * Baris yang sudah diputuskan hilang dari daftar seketika; pemuatan ulang hanya
     * menyegarkan saldo kartu lain dan berjalan di latar, supaya tombol tidak
     * menahan pengguna menunggu satu putaran jaringan penuh.
     */
    private fun selesai(report: WasteReview, pesan: String) {
        mutable.update { s ->
            s.copy(reports = s.reports.filterNot { it.id == report.id }, confirmation = null, rejecting = null, message = pesan)
        }
        job = viewModelScope.launch { load() }
    }
}
