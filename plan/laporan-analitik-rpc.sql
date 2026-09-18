-- Agregasi layar Laporan (modul Manager) di sisi server.
--
-- Jalankan BERURUTAN: Bagian 1 (indeks) lebih dulu, lalu Bagian 2 (fungsi).
--
-- LATAR: jalur lama menarik SELURUH baris `orders` beserta `order_items`-nya untuk
-- periode terpilih, 1000 baris per halaman secara berurutan, lalu menjumlahkannya di
-- HP. Versi pertama fungsi ini memakai CTE yang direferensi berulang; PostgreSQL
-- memateraikannya, hasil materialisasi tidak punya indeks maupun statistik, dan
-- planner memilih nested loop di atasnya -- 8 detik untuk 6 ribu pesanan, melewati
-- `statement_timeout` milik peran `authenticated`. Terbukti lambat juga saat dipanggil
-- dengan service_role, jadi RLS bukan penyebabnya.
--
-- Versi di bawah TIDAK memakai CTE bersama sama sekali: hanya join biasa antar tabel
-- nyata, empat pernyataan pendek yang masing-masing bisa direncanakan planner dengan
-- statistik penuh.

-- ============================================================================
-- BAGIAN 1 -- INDEKS
-- ============================================================================
-- CONCURRENTLY supaya pembuatannya tidak memblokir penulisan pesanan dari POS.
-- Ia TIDAK BOLEH berada di dalam blok transaksi: di SQL Editor Supabase, jalankan
-- SATU PER SATU, sendirian, bukan bersama pernyataan lain.
--
-- `order_items(order_id)` adalah yang paling menentukan. PostgreSQL TIDAK membuat
-- indeks otomatis untuk kolom foreign key, jadi tanpa ini setiap penggabungan ke
-- item memindai seluruh tabel item.

-- Jalankan sendirian:
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_order_items_order_id
  ON public.order_items (order_id);

-- Jalankan sendirian:
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_outlet_created_at
  ON public.orders (outlet_id, created_at);


-- ============================================================================
-- BAGIAN 2 -- FUNGSI
-- ============================================================================
-- SECURITY INVOKER: fungsi berjalan sebagai pemanggil, jadi RLS `orders_select_scoped`
-- tetap menyaring barisnya persis seperti saat tabelnya dibaca langsung. Tidak ada
-- aturan akses yang ditiru ulang di sini, jadi tidak ada yang bisa melenceng darinya.
--
-- Aturan uangnya menyalin `susunAnalitikLaporan()` di aplikasi, yang menyalin migrasi
-- web 20300128000000: hanya `completed` yang masuk hitungan uang; potongan =
-- MAX(0, nilai item - total_amount), dengan kolom diskon hanya untuk pesanan tanpa
-- baris item; nama menu digabung ke induk; jam memakai zona Jakarta.

CREATE OR REPLACE FUNCTION public.native_laporan_analitik(
  p_dari         timestamptz,
  p_sampai       timestamptz,
  p_channels     text[]  DEFAULT NULL,
  p_channel_null boolean DEFAULT false,
  p_payment      text    DEFAULT NULL,
  p_outlet       text    DEFAULT NULL
)
RETURNS jsonb
LANGUAGE plpgsql
STABLE
SECURITY INVOKER
SET search_path = public
AS $function$
DECLARE
  v_omzet    bigint := 0;
  v_potongan bigint := 0;
  v_subsidi  bigint := 0;
  v_sukses   int    := 0;
  v_batal    int    := 0;
  v_qty      int    := 0;
  v_bayar    jsonb  := '[]'::jsonb;
  v_jam      jsonb  := '[]'::jsonb;
  v_daftar   jsonb  := '[]'::jsonb;
BEGIN
  -- (1) Grain pesanan. Nilai item dijumlahkan lebih dulu pada subquery tersendiri,
  --     bukan lewat join langsung ke order_items: join langsung menggandakan baris
  --     pesanan sebanyak itemnya dan membuat total_amount terhitung berkali-kali.
  SELECT
    COALESCE(SUM(t.total) FILTER (WHERE t.status = 'completed'), 0),
    COALESCE(SUM(CASE WHEN t.nilai IS NULL
                      THEN t.disc + t.promo
                      ELSE GREATEST(0, t.nilai - t.total) END)
             FILTER (WHERE t.status = 'completed'), 0),
    COALESCE(SUM(t.promo) FILTER (WHERE t.status = 'completed'), 0),
    COUNT(*) FILTER (WHERE t.status = 'completed'),
    COUNT(*) FILTER (WHERE t.status = 'cancelled')
  INTO v_omzet, v_potongan, v_subsidi, v_sukses, v_batal
  FROM (
    SELECT o.status,
           COALESCE(o.total_amount, 0)    AS total,
           COALESCE(o.discount_amount, 0) AS disc,
           COALESCE(o.promo_subsidy, 0)   AS promo,
           i.nilai
    FROM public.orders o
    LEFT JOIN (
      SELECT oi.order_id, SUM(COALESCE(oi.subtotal, 0)) AS nilai
      FROM public.order_items oi
      JOIN public.orders o2 ON o2.id = oi.order_id
      WHERE o2.created_at >= p_dari
        AND o2.created_at <= p_sampai
        AND o2.status = 'completed'
      GROUP BY oi.order_id
    ) i ON i.order_id = o.id
    WHERE o.created_at >= p_dari
      AND o.created_at <= p_sampai
      AND (p_payment  IS NULL OR o.payment_method = p_payment)
      AND (p_channels IS NULL OR o.channel = ANY (p_channels))
      AND (NOT p_channel_null OR o.channel IS NULL)
      AND (p_outlet   IS NULL OR o.outlet_id::text = p_outlet)
  ) t;

  -- (2) Rincian metode bayar.
  SELECT COALESCE(
           jsonb_agg(jsonb_build_object('metode', r.metode, 'jumlah', r.jumlah, 'omzet', r.omzet)
                     ORDER BY r.omzet DESC),
           '[]'::jsonb)
  INTO v_bayar
  FROM (
    SELECT COALESCE(NULLIF(btrim(o.payment_method), ''), 'unknown') AS metode,
           COUNT(*)::int                                            AS jumlah,
           SUM(COALESCE(o.total_amount, 0))::bigint                 AS omzet
    FROM public.orders o
    WHERE o.created_at >= p_dari
      AND o.created_at <= p_sampai
      AND o.status = 'completed'
      AND (p_payment  IS NULL OR o.payment_method = p_payment)
      AND (p_channels IS NULL OR o.channel = ANY (p_channels))
      AND (NOT p_channel_null OR o.channel IS NULL)
      AND (p_outlet   IS NULL OR o.outlet_id::text = p_outlet)
    GROUP BY 1
  ) r;

  -- (3) Sebaran per jam Jakarta. Selalu 24 angka, termasuk jam yang kosong.
  SELECT COALESCE(jsonb_agg(COALESCE(c.n, 0) ORDER BY j.jam), '[]'::jsonb)
  INTO v_jam
  FROM generate_series(0, 23) AS j(jam)
  LEFT JOIN (
    SELECT EXTRACT(hour FROM (o.created_at AT TIME ZONE 'Asia/Jakarta'))::int AS jam,
           COUNT(*)::int                                                      AS n
    FROM public.orders o
    WHERE o.created_at >= p_dari
      AND o.created_at <= p_sampai
      AND o.status = 'completed'
      AND (p_payment  IS NULL OR o.payment_method = p_payment)
      AND (p_channels IS NULL OR o.channel = ANY (p_channels))
      AND (NOT p_channel_null OR o.channel IS NULL)
      AND (p_outlet   IS NULL OR o.outlet_id::text = p_outlet)
    GROUP BY 1
  ) c ON c.jam = j.jam;

  -- (4) Grain item. `Nama | Varian` digabung ke menu induk.
  SELECT COALESCE(
           jsonb_agg(jsonb_build_object('nama', d.nama, 'qty', d.qty, 'omzet', d.omzet)
                     ORDER BY d.qty DESC),
           '[]'::jsonb),
         COALESCE(SUM(d.qty), 0)::int
  INTO v_daftar, v_qty
  FROM (
    SELECT CASE WHEN position('|' in COALESCE(oi.menu_item_name, '')) > 0
                THEN btrim(split_part(oi.menu_item_name, '|', 1))
                ELSE COALESCE(NULLIF(btrim(oi.menu_item_name), ''), 'Item')
           END                                   AS nama,
           SUM(COALESCE(oi.quantity, 0))::int    AS qty,
           SUM(COALESCE(oi.subtotal, 0))::bigint AS omzet
    FROM public.order_items oi
    JOIN public.orders o ON o.id = oi.order_id
    WHERE o.created_at >= p_dari
      AND o.created_at <= p_sampai
      AND o.status = 'completed'
      AND (p_payment  IS NULL OR o.payment_method = p_payment)
      AND (p_channels IS NULL OR o.channel = ANY (p_channels))
      AND (NOT p_channel_null OR o.channel IS NULL)
      AND (p_outlet   IS NULL OR o.outlet_id::text = p_outlet)
    GROUP BY 1
  ) d;

  RETURN jsonb_build_object(
    'omzet_bersih',      v_omzet,
    'omzet_kotor',       v_omzet + v_potongan,
    'potongan_merchant', v_potongan,
    'subsidi_platform',  v_subsidi,
    'pesanan_sukses',    v_sukses,
    'pesanan_batal',     v_batal,
    'item_terjual',      v_qty,
    'rata_rata_per_order',
      CASE WHEN v_sukses > 0 THEN round(v_omzet::numeric / v_sukses)::bigint ELSE 0 END,
    'rincian_pembayaran', v_bayar,
    'per_jam',            v_jam,
    'daftar_item',        v_daftar
  );
END;
$function$;

GRANT EXECUTE ON FUNCTION public.native_laporan_analitik(
  timestamptz, timestamptz, text[], boolean, text, text
) TO authenticated;

-- Verifikasi -- harus kembali dalam hitungan milidetik:
--   EXPLAIN (ANALYZE, BUFFERS)
--   SELECT public.native_laporan_analitik(
--     '2026-09-10T00:00:00+07:00'::timestamptz,
--     '2026-09-16T23:59:59.999+07:00'::timestamptz
--   );
--
-- CATATAN KOLOM: fungsi ini mengasumsikan `order_items.order_id` sebagai kunci tamu
-- ke `orders.id` -- nama yang dipakai PostgREST saat menyematkan `order_items` di
-- jalur lama. Bila di skema Anda namanya lain, ganti di langkah (1) dan (4).
--
-- MEMBATALKAN: DROP FUNCTION public.native_laporan_analitik(
--   timestamptz, timestamptz, text[], boolean, text, text);
-- Aplikasi otomatis kembali ke jalur baris mentah; tidak ada data yang perlu dipulihkan.
