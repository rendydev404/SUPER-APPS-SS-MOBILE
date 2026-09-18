-- Mode offline: kunci idempotensi untuk aksi tulis yang boleh menunggu di antrean perangkat.
--
-- Aksi yang diantre PASTI akan dicoba ulang: sinyal outlet putus-nyambung, proses aplikasi
-- dimatikan sistem di tengah pengiriman, WorkManager membangunkan ulang pekerjaannya. Tanpa
-- kunci ini, "kirim ulang karena tidak yakin sudah sampai" berubah jadi kasbon dobel, cuti
-- dobel, dan waste yang memotong stok dua kali.
--
-- Kuncinya dibuat KLIEN (UUID = id baris di tabel `outbox` perangkat) dan tetap sama di
-- setiap percobaan. Itu sebabnya ia menjadi penentu, bukan stempel waktu atau kombinasi
-- kolom: dua kasbon dengan nominal sama di menit yang sama adalah hal yang wajar, dan
-- tidak boleh dianggap duplikat.
--
-- Indeks unik sengaja PARSIAL (WHERE client_op_id IS NOT NULL): seluruh baris yang sudah
-- ada bernilai NULL, dan web tidak mengirim kolom ini sama sekali.

SET lock_timeout = '5s';

-- === Tabel dengan INSERT langsung dari klien ===

ALTER TABLE cash_advances      ADD COLUMN IF NOT EXISTS client_op_id uuid;
ALTER TABLE leave_requests     ADD COLUMN IF NOT EXISTS client_op_id uuid;
ALTER TABLE stok_waste_reports ADD COLUMN IF NOT EXISTS client_op_id uuid;
ALTER TABLE ledger_stok        ADD COLUMN IF NOT EXISTS client_op_id uuid;
ALTER TABLE permintaan_bahan   ADD COLUMN IF NOT EXISTS client_op_id uuid;

CREATE UNIQUE INDEX IF NOT EXISTS cash_advances_client_op_id_key
  ON cash_advances (client_op_id) WHERE client_op_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS leave_requests_client_op_id_key
  ON leave_requests (client_op_id) WHERE client_op_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS stok_waste_reports_client_op_id_key
  ON stok_waste_reports (client_op_id) WHERE client_op_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ledger_stok_client_op_id_key
  ON ledger_stok (client_op_id) WHERE client_op_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS permintaan_bahan_client_op_id_key
  ON permintaan_bahan (client_op_id) WHERE client_op_id IS NOT NULL;

COMMENT ON COLUMN cash_advances.client_op_id IS
  'UUID dari antrean offline perangkat. Sama di setiap percobaan kirim; NULL untuk kiriman web.';

-- === RPC permintaan ===
--
-- Diturunkan dari 20300105000011_permintaan_auto_cancel_stale.sql (definisi live per
-- 2026-09-18). Yang berubah hanya dua: parameter p_client_op_id, dan penjaga "sudah pernah
-- diproses" di awal.
--
-- Penjaganya WAJIB di dalam fungsi, bukan mengandalkan indeks unik saja: fungsi ini juga
-- membatalkan permintaan lama sebelum menyisipkan yang baru, jadi percobaan kedua yang
-- gagal di INSERT tetap sudah terlanjur membatalkan permintaan orang.
--
-- PERIKSA SEBELUM DIJALANKAN: kalau buat_permintaan_svc sudah diubah lagi setelah tanggal
-- itu, turunkan ulang dari versi terbaru alih-alih menjalankan yang ini.

CREATE OR REPLACE FUNCTION buat_permintaan_svc(
    p_outlet_id uuid,
    p_items json,
    p_dibuat_oleh uuid,
    p_target_metadata jsonb DEFAULT '[]'::jsonb,
    p_client_op_id uuid DEFAULT NULL
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_permintaan_id uuid;
    v_item json;
    v_bahan_ids uuid[];
BEGIN
    -- Kiriman ulang dari antrean offline: permintaannya sudah masuk sebelum koneksi putus.
    -- Diam-diam sukses supaya antrean menghapus barisnya, bukan mengulang terus.
    IF p_client_op_id IS NOT NULL AND EXISTS (
        SELECT 1 FROM permintaan_bahan WHERE client_op_id = p_client_op_id
    ) THEN
        RETURN;
    END IF;

    IF p_items IS NULL OR json_array_length(p_items) = 0 THEN
        RAISE EXCEPTION 'permintaan harus berisi minimal 1 item';
    END IF;

    SELECT array_agg((elem->>'bahan_baku_id')::uuid)
    INTO v_bahan_ids
    FROM json_array_elements(p_items) elem;

    -- Auto-batalkan permintaan lama (>12 jam, masih menunggu) di outlet ini
    -- yang mengandung bahan yang sama dengan permintaan baru ini.
    UPDATE permintaan_bahan pb
    SET status = 'dibatalkan',
        catatan_kitchen = 'Dibatalkan otomatis: diajukan ulang setelah >12 jam menunggu',
        updated_at = NOW()
    WHERE pb.outlet_id = p_outlet_id
      AND pb.status = 'menunggu'
      AND pb.created_at < NOW() - INTERVAL '12 hours'
      AND EXISTS (
        SELECT 1 FROM permintaan_bahan_item pbi
        WHERE pbi.permintaan_id = pb.id
          AND pbi.bahan_baku_id = ANY(v_bahan_ids)
      );

    INSERT INTO permintaan_bahan (outlet_id, dibuat_oleh, status, target_metadata, client_op_id)
    VALUES (p_outlet_id, p_dibuat_oleh, 'menunggu', p_target_metadata, p_client_op_id)
    RETURNING id INTO v_permintaan_id;

    FOR v_item IN SELECT * FROM json_array_elements(p_items)
    LOOP
        INSERT INTO permintaan_bahan_item (permintaan_id, bahan_baku_id, qty_diminta)
        VALUES (
            v_permintaan_id,
            (v_item->>'bahan_baku_id')::uuid,
            (v_item->>'qty_diminta')::numeric
        );
    END LOOP;
END;
$$;

-- Versi 4-parameter HARUS dibuang. Dua fungsi bernama sama yang sama-sama cocok dengan
-- pemanggilan 4 argumen membuat PostgREST gagal memilih — persis masalah yang dulu
-- diperbaiki migration 20300105000001_fix_buat_permintaan_svc_stale_overload.sql.
DROP FUNCTION IF EXISTS buat_permintaan_svc(uuid, json, uuid, jsonb);

GRANT EXECUTE ON FUNCTION buat_permintaan_svc(uuid, json, uuid, jsonb, uuid) TO authenticated;

NOTIFY pgrst, 'reload schema';

-- DOWN:
-- (kolom & indeks aman ditinggal; yang perlu dikembalikan hanya fungsinya)
-- DROP FUNCTION IF EXISTS buat_permintaan_svc(uuid, json, uuid, jsonb, uuid);
-- lalu jalankan ulang 20300105000011_permintaan_auto_cancel_stale.sql
