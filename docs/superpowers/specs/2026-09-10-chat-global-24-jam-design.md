# Chat Global 24 Jam — Desain

Tanggal: 2026-09-10. Status: disetujui pemilik produk (sesi Claude Code).

## Tujuan

Satu grup chat perusahaan untuk semua pemegang akun staff, rasa grup WhatsApp
dengan estetika iOS: bubble, nama tampilan + foto profil pengirim, reply/quote,
kirim foto (WebP), typing indicator beranimasi. Pesan bersifat sementara —
hilang setelah 24 jam.

Prinsip keras: **semua perubahan database aditif**; tidak ada tabel, policy,
trigger, RPC, atau bucket yang dipakai web/POS yang diubah. Jalur
`postgres_changes` yang sudah dipakai modul lain tidak diubah satu baris pun.

## Keputusan produk

- **Satu grup global** — semua role staff, lintas outlet. Bukan per outlet.
- **Jenis pesan**: teks (maks 2000 karakter), reply/quote, foto WebP
  (sisi terpanjang 1600 px, kualitas ~85 — tajam tapi ringan).
- **24 jam**: klien hanya menampilkan pesan berumur < 24 jam; job `pg_cron`
  per jam menghapus baris + objek storage yang kedaluwarsa. Pesan hidup penuh
  24 jam — job per jam hanya petugas pembersihnya.
- **Hapus pesan sendiri**: long-press → hapus (DELETE beneran, bukan tanda).
- **Mitra**: rute terdaftar global, tapi jalan masuk UI fase ini hanya di
  Beranda staff. (Pengirim wajib punya baris `outlet_staff` — mitra punya.)

## Database (migration di repo web, sumber kebenaran schema)

File: `supabase/migrations/20300209000000_chat_global_24h.sql`.

- **Tabel `public.chat_messages`**: `id`, `sender_id`, snapshot pengirim
  (`sender_name`, `sender_avatar`), `body`, `image_path`, snapshot reply
  (`reply_to_id`, `reply_to_name`, `reply_to_snippet`), `created_at`.
  Constraint: body tidak kosong ATAU ada foto.
- **Snapshot lewat trigger BEFORE INSERT `SECURITY DEFINER`**, bukan join saat
  baca. Alasan: (1) RLS `outlet_staff` tidak mengizinkan membaca baris orang
  lain, dan menambah policy SELECT lebar demi chat justru mengusik tabel paling
  sensitif; (2) pesan cuma hidup 24 jam, snapshot tidak sempat basi lama;
  (3) nilainya diisi server dari `auth.uid()`, tidak bisa dipalsukan klien.
  Trigger juga memvalidasi `image_path` wajib berawalan
  `chat-media/<uid>/` dan mengisi snapshot reply dari pesan asal.
- **RLS**: SELECT `authenticated` hanya baris < 24 jam; INSERT hanya
  `sender_id = auth.uid()`; DELETE hanya pesan sendiri; tanpa UPDATE.
- **Realtime**: `ALTER PUBLICATION supabase_realtime ADD TABLE chat_messages`
  (idempoten) + `REPLICA IDENTITY FULL` supaya event DELETE lolos filter RLS
  walrus dan layar lain ikut menghapus bubble.
- **Bucket `chat-media`**: privat, 5 MB, `image/webp` + `image/jpeg`; policy
  INSERT wajib folder `auth.uid()`, SELECT untuk `authenticated` — pola persis
  bucket `avatars`.
- **pg_cron per jam**: hapus `chat_messages` dan baris `storage.objects`
  `chat-media` yang berumur > 24 jam.

Migration ditulis + di-commit di repo web; **penerapan ke database produksi
menunggu konfirmasi pemilik**.

## Realtime (native, `core:network`)

- Pesan baru/terhapus: layar memakai `Realtime.updates("chat_messages")` yang
  sudah ada → sinyal "berubah" → muat ulang lewat REST. Tidak ada parsing
  payload realtime (keputusan lama repo ini, tetap dihormati).
- Typing indicator: **broadcast, tanpa database**. `Realtime` mendapat API
  tambahan `broadcasts(topic)` (Flow payload) + `sendBroadcast(topic, event,
  payload)`, menumpang socket & mesin join yang sama; kunci channel broadcast
  diberi awalan berbeda sehingga tidak mungkin bertabrakan dengan nama tabel.
  Jalur `updates()` tidak berubah. Bonus: fondasi presence/broadcast yang juga
  dibutuhkan tab Live Location manager kelak.
- Protokol typing: klien mengirim event `typing` `{id, nama}` maksimal 1× per
  3 detik selama mengetik; penerima menganggap orang itu berhenti setelah 5
  detik tanpa sinyal. Lossy tidak apa-apa.

## Modul `feature:chat` (native)

Struktur meniru `feature:profil`:

- `data/ChatModels.kt` — model + parsing JSON.
- `data/ChatRepository.kt` — ambil pesan (< 24 jam, urut naik), kirim teks/
  reply, unggah foto WebP ke `chat-media/<uid>/<uuid>.webp` via `StorageUtil`,
  hapus pesan sendiri.
- `domain/` — logika murni & teruji: pengelompokan bubble berurutan per
  pengirim, pemisah tanggal, throttle typing, potong snippet reply.
- `ui/ChatViewModel.kt` — state pesan + antrean kirim optimis (jam pasir →
  centang / gagal+ulangi), reply target, daftar pengetik; refetch saat sinyal
  realtime; kirim sinyal typing.
- `ui/ChatScreen.kt` — estetika iOS, UX WhatsApp:
  - Bubble kanan (milik sendiri, tanpa nama) vs kiri (avatar `AvatarStaf` +
    nama berwarna konsisten per pengirim). Sudut bubble asimetris ala iMessage.
  - Pesan berurutan dikelompokkan; avatar hanya di pesan pertama grup.
  - Swipe-untuk-reply + haptic; kartu kutipan di atas kotak ketik; bubble
    reply menampilkan kutipan.
  - Typing indicator: bubble tiga titik beranimasi + nama.
  - Animasi masuk pesan, tombol scroll-ke-bawah dengan badge, pemisah
    tanggal, jam kecil di pojok bubble, banner "Pesan otomatis terhapus
    setelah 24 jam".
  - Kirim foto: Photo Picker → kompres WebP → kirim optimis dengan progres.
  - Long-press: Balas / Salin / Hapus (hapus hanya pesan sendiri).
  - Foto chat dimuat dengan `ImageLoader` bersama `AvatarStorage.imageLoader`
    (bucket privat → butuh Authorization; loader itu sudah membawanya).

## Penyambungan

- `settings.gradle.kts`: `include(":feature:chat")`.
- `app/build.gradle.kts`: dependensi modul.
- `MainActivity`: `Routes.CHAT` didaftarkan seperti `PROFIL` (global).
- `HomeScreen`: satu jalan masuk chat untuk semua role. CATATAN: file ini
  sedang berisi perubahan lokal pemilik yang belum di-commit — disunting
  seperlunya, TIDAK ikut di-commit.

## Galat & offline

Kirim gagal → bubble ditandai gagal, tap untuk ulangi (antrean di memori,
tanpa DB lokal — umur pesan 24 jam tidak menjustifikasi Room). Socket putus →
`updates()` memancarkan ulang saat pulih dan layar memuat ulang sendiri.

## Testing

Unit test JVM: pengelompokan bubble & pemisah tanggal, filter/parse pesan,
throttle typing, pembentukan snippet reply, validasi path foto. Build via
`gradlew.bat` dari PowerShell.
