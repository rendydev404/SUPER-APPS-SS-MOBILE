# Plan Perbaikan Liveness Head Turn

## Ringkasan

Liveness berhenti pada instruksi **"Tolehkan kepala ke kiri/kanan"** walaupun wajah dan contour berhasil terdeteksi. Akar masalah utama berada pada konfigurasi ML Kit Face Detection: kombinasi `PERFORMANCE_MODE_FAST`, `LANDMARK_MODE_NONE`, `CONTOUR_MODE_ALL`, dan `CLASSIFICATION_MODE_NONE` tidak menghasilkan sudut Euler. Akibatnya `headEulerAngleY` tidak memberikan perubahan yaw yang dibutuhkan state machine liveness.

Plan ini sengaja memisahkan perbaikan fungsional utama, pembuktian pada perangkat, perbaikan UX, dan isu multi-face agar setiap perubahan dapat diuji secara independen.

## Status Investigasi

- Fase root-cause investigation: selesai secara statis, perlu konfirmasi runtime pada perangkat setelah instrumentation tersedia.
- Implementasi: belum dimulai.
- File aplikasi yang diubah saat investigasi: tidak ada.
- Referensi resmi: [ML Kit Face Detection Concepts](https://developers.google.com/ml-kit/vision/face-detection/face-detection-concepts).

## Gejala dan Reproduksi

### Gejala

- Preview kamera depan aktif.
- Satu wajah berhasil terdeteksi.
- Face contour/mesh tampil mengikuti wajah.
- Instruksi liveness tampil, misalnya `Tolehkan kepala ke kiri`.
- Setelah kepala ditolehkan, proses tidak melanjutkan ke submit absensi.

### Langkah reproduksi

1. Buka halaman absensi dengan kamera depan aktif.
2. Tunggu identifikasi wajah selesai.
3. Tunggu phase berubah menjadi `ClockPhase.LIVENESS`.
4. Ikuti tantangan menoleh ke kiri atau kanan.
5. Kembalikan wajah menghadap depan.
6. Amati bahwa instruksi tidak berubah dan proses tidak masuk ke submit.

## Bukti Akar Masalah

### Konfigurasi detector

File:

`core/camera/src/main/java/com/sukashawarma/superapp/core/camera/face_data/FaceDetectionAnalyzer.kt`

Konfigurasi saat ini:

```kotlin
FaceDetectorOptions.Builder()
    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
    .enableTracking()
    .build()
```

Dokumentasi ML Kit menyatakan bahwa sudut Euler X/Y/Z tidak dilaporkan ketika empat opsi utama tersebut digunakan bersamaan.

### Aliran kegagalan

1. ML Kit berhasil menghasilkan wajah dan contour.
2. Analyzer membaca `face.headEulerAngleY` dan membalik tandanya untuk menyesuaikan perspektif preview kamera depan:

   ```kotlin
   val signal = FaceSignal(
       yawDeg = -face.headEulerAngleY,
       faceCount = faces.size,
   )
   ```

3. `LivenessDetector` mensyaratkan:

   - kiri: `yawDeg < -15f`;
   - kanan: `yawDeg > 15f`;
   - kembali frontal: `abs(yawDeg) < 8f`.

4. Karena konfigurasi detector tidak menyediakan Euler angle, yaw tidak mencapai ambang gerakan.
5. State internal detector tidak berpindah dari phase menunggu gerakan ke phase menunggu wajah frontal.
6. `detector.feed(...)` terus mengembalikan `false`, sehingga `doSubmit()` tidak pernah dipanggil.

## Hipotesis yang Akan Diuji

> Mengubah hanya performance mode dari `PERFORMANCE_MODE_FAST` menjadi `PERFORMANCE_MODE_ACCURATE` akan membuat ML Kit kembali menyediakan `headEulerAngleY`, sehingga yaw dapat melewati ambang ±15 derajat dan state machine liveness dapat selesai setelah wajah kembali frontal.

Perubahan awal harus hanya mengubah satu variabel. Jangan sekaligus mengubah ambang, tanda yaw, landmark mode, state machine, dan UI karena hasil pengujian tidak lagi dapat mengisolasi penyebab.

## Ruang Lingkup

### Termasuk

- Instrumentation sementara untuk membuktikan nilai raw Euler Y, normalized yaw, challenge, dan phase liveness.
- Perubahan minimum `FAST` menjadi `ACCURATE`.
- Pengujian tantangan kiri dan kanan pada kamera depan.
- Pengujian bahwa pengguna wajib kembali frontal sebelum submit.
- Unit test state machine `LivenessDetector`.
- Setelah fungsi utama terbukti, perbaikan teks agar phase kedua meminta pengguna kembali menghadap depan.
- Penghapusan atau penonaktifan log diagnostik verbose sebelum finalisasi.

### Tidak termasuk dalam perubahan utama

- Perombakan face recognition atau embedding.
- Perubahan threshold tanpa bukti runtime.
- Penggantian library kamera atau ML Kit.
- Penanganan multi-face saat `CONTOUR_MODE_ALL` aktif.
- Refactor UI kamera yang tidak terkait liveness.

## Rencana Implementasi

### Tahap 1 — Tambahkan observability sementara

Tambahkan log diagnostik terbatas pada build debug di batas antara ML Kit dan state machine. Log setidaknya memuat:

- `face.headEulerAngleY` mentah;
- `FaceSignal.yawDeg` setelah normalisasi tanda;
- jumlah wajah;
- challenge aktif;
- phase liveness: menunggu gerakan atau menunggu frontal;
- hasil `acted`, `isFrontal`, dan `passed`.

Contoh bentuk informasi, bukan implementasi final:

```text
rawEulerY=18.4 normalizedYaw=-18.4 challenge=TURN_LEFT phase=WAITING_TURN
normalizedYaw=-18.4 acted=true phase=WAITING_FRONTAL
normalizedYaw=2.1 isFrontal=true passed=true
```

Instrumentation harus tidak mencatat foto, embedding, identitas biometrik, atau data sensitif lainnya.

### Tahap 2 — Buat baseline sebelum perbaikan

Jalankan build debug pada perangkat yang mereproduksi bug:

1. Rekam yaw saat wajah frontal selama beberapa frame.
2. Rekam yaw saat menoleh kiri.
3. Rekam yaw saat menoleh kanan.
4. Pastikan detector tetap menerima tepat satu wajah.
5. Simpan rentang nilai sebagai bukti baseline.

Ekspektasi baseline: contour tersedia, tetapi Euler Y/yaw tidak berubah secara memadai untuk melewati ±15 derajat.

### Tahap 3 — Terapkan satu perubahan minimum

Di `FaceDetectionAnalyzer.kt`, ubah hanya:

```kotlin
.setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
```

menjadi:

```kotlin
.setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
```

Pertahankan sementara opsi lain agar hasil pengujian hanya mengukur dampak perubahan performance mode.

### Tahap 4 — Verifikasi hipotesis pada perangkat

Ulangi pengukuran dengan skenario berikut:

#### Tantangan kiri

1. Mulai dari posisi frontal.
2. Pastikan yaw stabil dekat nol.
3. Tolehkan kepala ke kiri pengguna.
4. Pastikan normalized yaw melewati `-15f`.
5. Pastikan detector berpindah ke phase menunggu frontal.
6. Kembalikan kepala ke tengah.
7. Pastikan `abs(yaw) < 8f` dan `passed=true`.
8. Pastikan submit hanya dipanggil satu kali.

#### Tantangan kanan

1. Mulai dari posisi frontal.
2. Tolehkan kepala ke kanan pengguna.
3. Pastikan normalized yaw melewati `15f`.
4. Kembalikan kepala ke tengah.
5. Pastikan liveness lolos dan submit hanya dipanggil satu kali.

#### Kasus negatif

- Tetap frontal tidak boleh langsung lolos.
- Bergerak kurang dari ambang 15 derajat tidak boleh dianggap sebagai turn.
- Menoleh ke arah yang berlawanan dari challenge tidak boleh lolos.
- Setelah turn berhasil, tetap dalam posisi menoleh tidak boleh submit sebelum kembali frontal.
- Wajah hilang tidak boleh melanjutkan state.

### Tahap 5 — Tambahkan unit test state machine

Tambahkan test murni Kotlin untuk `LivenessDetector` minimal mencakup:

1. `TURN_LEFT`: `0 -> -16 -> 0` menghasilkan `false -> false -> true`.
2. `TURN_RIGHT`: `0 -> 16 -> 0` menghasilkan `false -> false -> true`.
3. Gerakan di batas `15` tidak lolos karena operator yang digunakan bersifat strict (`<`/`>`).
4. Gerakan ke arah yang salah tidak lolos.
5. Kembali frontal tanpa pernah melakukan turn tidak lolos.
6. `faceCount=0` dan `faceCount>1` tidak lolos.
7. Setelah `passed=true`, pemanggilan berikutnya tetap `true`.

Catatan: unit test ini memverifikasi state machine, tetapi tidak dapat membuktikan bahwa konfigurasi ML Kit benar-benar menghasilkan Euler angle. Verifikasi tersebut tetap membutuhkan perangkat/emulator dengan input kamera yang representatif.

### Tahap 6 — Perbaiki feedback phase kedua

Saat ini label challenge tetap sama setelah turn terdeteksi. Ini membingungkan karena detector sebenarnya menunggu pengguna kembali frontal.

Setelah perbaikan fungsional terbukti:

1. Ekspos status liveness yang semantik, misalnya `WAITING_TURN`, `WAITING_FRONTAL`, dan `PASSED`.
2. Tampilkan challenge asli saat `WAITING_TURN`.
3. Tampilkan `Kembalikan wajah menghadap depan` saat `WAITING_FRONTAL`.
4. Jangan mengubah state machine dan UI dalam commit/perubahan pengujian yang sama dengan perubahan `ACCURATE`, kecuali perubahan pertama sudah diverifikasi.

### Tahap 7 — Evaluasi performa

Karena `PERFORMANCE_MODE_ACCURATE` lebih berat daripada `FAST`, ukur pada perangkat target:

- frame processing latency;
- apakah preview tersendat;
- suhu/CPU secara kasar selama penggunaan berulang;
- kestabilan interval liveness yang saat ini ditargetkan sekitar 5 FPS;
- apakah `STRATEGY_KEEP_ONLY_LATEST` tetap mencegah antrean frame.

Kriteria awal: deteksi liveness responsif tanpa freeze dan tidak menyebabkan backlog frame.

Jika mode `ACCURATE` memperbaiki yaw tetapi performanya tidak dapat diterima, hentikan dan bentuk hipotesis baru. Jangan langsung mengubah beberapa opsi. Kandidat eksperimen berikutnya adalah konfigurasi detector terpisah untuk kebutuhan contour dan liveness, tetapi itu merupakan perubahan arsitektur yang lebih besar dan harus dibahas terlebih dahulu.

### Tahap 8 — Bersihkan instrumentation

- Hapus log per-frame yang verbose atau batasi secara ketat pada debug build.
- Jangan menyisakan logging biometrik/data pribadi.
- Pertahankan hanya telemetry ringan yang benar-benar diperlukan untuk diagnosis produksi.
- Jalankan kembali test setelah cleanup.

## Acceptance Criteria

Perbaikan dinyatakan selesai bila seluruh kondisi berikut terpenuhi:

- Tantangan kiri lolos setelah yaw melewati `-15f` lalu kembali ke rentang frontal.
- Tantangan kanan lolos setelah yaw melewati `15f` lalu kembali ke rentang frontal.
- Menoleh ke arah yang salah tidak meloloskan challenge.
- Tetap frontal tidak meloloskan challenge.
- Submit tidak terjadi saat kepala masih menoleh.
- Submit hanya dipanggil satu kali setelah liveness lolos.
- Face contour tetap tampil.
- Preview kamera tidak freeze atau mengalami penurunan performa yang tidak dapat diterima.
- Semua unit test liveness lulus.
- Test terkait absensi yang sudah ada tetap lulus.
- UI memberi tahu pengguna untuk kembali menghadap depan setelah turn terdeteksi.
- Tidak ada log sensitif/per-frame verbose dalam build final.

## Risiko dan Mitigasi

| Risiko | Dampak | Mitigasi |
| --- | --- | --- |
| `ACCURATE` meningkatkan latency | Respons liveness lebih lambat | Ukur pada perangkat target; pertahankan resolusi dan backpressure saat eksperimen awal |
| Threshold 15 derajat tidak cocok untuk semua perangkat | False reject atau terlalu mudah lolos | Kumpulkan nilai yaw nyata terlebih dahulu; ubah threshold hanya berdasarkan bukti |
| Pembalikan tanda berbeda pada perangkat/orientasi tertentu | Tantangan kiri/kanan tertukar | Uji kedua arah pada kamera depan dan seluruh orientasi yang memang didukung aplikasi |
| Label tetap meminta menoleh setelah gerakan berhasil | Pengguna tidak tahu harus kembali frontal | Tambahkan status `WAITING_FRONTAL` setelah fungsi utama terverifikasi |
| Contour mode membatasi deteksi ke satu wajah | Guard `faceCount > 1` tidak efektif | Catat sebagai pekerjaan terpisah; jangan gabungkan dengan fix yaw |
| Logging per-frame memenuhi logcat atau membuka data sensitif | Noise/permasalahan privasi | Gunakan debug-only, throttle, dan jangan log citra/identitas |

## Isu Terpisah: Multi-face dan Tracking

Dokumentasi ML Kit menyebutkan bahwa saat contour detection aktif, hanya satu wajah yang dideteksi. Karena itu:

- `faces.size > 1` tidak dapat diandalkan untuk mendeteksi orang tambahan;
- `enableTracking()` tidak banyak berguna ketika contour mode aktif;
- pesan `Cukup satu wajah` kemungkinan tidak pernah terpicu berdasarkan `faces.size`.

Masalah ini tidak menyebabkan liveness macet, sehingga tidak boleh digabungkan ke perubahan utama. Buat investigasi/plan terpisah bila requirement keamanan memang mengharuskan deteksi lebih dari satu wajah.

## Strategi Rollback

Jika perubahan `ACCURATE` menyebabkan regresi performa atau tidak menghasilkan yaw:

1. Kembalikan hanya perubahan performance mode ke `FAST`.
2. Pertahankan bukti log dan hasil pengukuran tanpa data sensitif.
3. Nyatakan hipotesis pertama gagal.
4. Kembali ke investigasi konfigurasi ML Kit dan evaluasi detector terpisah untuk liveness.
5. Jangan menurunkan threshold atau membalik tanda yaw sebagai kompensasi tanpa bukti.

## Urutan Commit yang Disarankan

1. Test/instrumentation untuk mereproduksi dan mengukur yaw.
2. Perubahan minimum `FAST` menjadi `ACCURATE` beserta hasil verifikasi.
3. Unit test state machine yang lengkap jika belum masuk pada commit test pertama.
4. Perbaikan UX phase `WAITING_FRONTAL`.
5. Cleanup logging dan dokumentasi hasil pengujian.

Isu multi-face dibuat sebagai pekerjaan dan commit terpisah.

## Checklist Eksekusi

- [ ] Tambahkan instrumentation debug yang aman.
- [ ] Rekam baseline yaw sebelum perubahan.
- [ ] Tambahkan failing/reproduction test untuk state machine.
- [ ] Ubah hanya `PERFORMANCE_MODE_FAST` menjadi `PERFORMANCE_MODE_ACCURATE`.
- [ ] Verifikasi yaw kiri pada perangkat.
- [ ] Verifikasi yaw kanan pada perangkat.
- [ ] Verifikasi kembali frontal sebelum submit.
- [ ] Verifikasi kasus negatif.
- [ ] Jalankan unit test dan regression test absensi.
- [ ] Ukur dampak performa.
- [ ] Tambahkan feedback `Kembalikan wajah menghadap depan`.
- [ ] Bersihkan log diagnostik verbose.
- [ ] Dokumentasikan hasil akhir dan isu multi-face secara terpisah.
