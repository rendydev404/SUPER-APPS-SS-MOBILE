-- Jalankan di Supabase SQL Editor. Salin juga ke repo web sebagai
-- supabase/migrations/20300126000001_permintaan_estimasi_budget_scoped_rpcs.sql
-- supaya riwayat migration tetap konsisten.
--
-- Tiga RPC berpagar untuk klien yang memakai JWT user (aplikasi native), padanan
-- Server Action ber-service-role di apps/stok/src/app/actions/budget.ts:
--
--   estimateCartValue        -> estimasi_nilai_keranjang(p_items)
--   getOutletBudgetStatus    -> get_outlet_budget_status_scoped(p_outlet_id)
--   requestBudgetTopupAction -> request_budget_topup_scoped(p_outlet_id, ...)
--
-- Kenapa pembungkus, bukan GRANT langsung: fungsi aslinya SECURITY DEFINER TANPA
-- pemeriksaan pemanggil (web memagarinya di Server Action lewat
-- assertOutletAccessible / requireActiveStaff). Membuka GRANT berarti siapa pun
-- yang punya sesi bisa membaca budget atau mengajukan top-up untuk outlet mana
-- pun. Pembungkus ini memasang pagar yang sama, lalu mendelegasikan.
--
-- Kenapa estimasi lewat RPC, bukan melonggarkan RLS bahan_baku_harga: web
-- menampilkan TOTAL estimasi ke semua staf aktif, tetapi harga beli per bahan
-- tetap hanya untuk admin/owner/kitchen/purchasing/admin_finance (policy
-- bbh_read). RPC ini menjaga pemisahan itu — yang keluar hanya agregat.

-- ---------------------------------------------------------------------------
-- 1. Estimasi nilai keranjang — cermin estimateCartValue.
--    p_items: [{"bahan_baku_id": uuid, "qty": numeric}] (qty = satuan distribusi)
--    Hasil: {"total_nilai": n, "item_tanpa_harga": [uuid], "kategori_nilai": {kat: n}}
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.estimasi_nilai_keranjang(p_items jsonb)
RETURNS jsonb
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_total          NUMERIC := 0;
  v_tanpa_harga    jsonb   := '[]'::jsonb;
  v_kategori_nilai jsonb   := '{}'::jsonb;
  v_item           jsonb;
  v_bahan_id       uuid;
  v_qty            NUMERIC;
  v_harga          NUMERIC;
  v_kategori       TEXT;
  v_subtotal       NUMERIC;
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM outlet_staff WHERE id = auth.uid() AND status = 'active'
  ) THEN
    RAISE EXCEPTION 'Forbidden: akun tidak aktif';
  END IF;

  IF p_items IS NULL OR jsonb_typeof(p_items) <> 'array' OR jsonb_array_length(p_items) = 0 THEN
    RETURN jsonb_build_object(
      'total_nilai', 0, 'item_tanpa_harga', '[]'::jsonb, 'kategori_nilai', '{}'::jsonb
    );
  END IF;

  FOR v_item IN SELECT * FROM jsonb_array_elements(p_items)
  LOOP
    v_bahan_id := (v_item->>'bahan_baku_id')::uuid;
    v_qty      := COALESCE((v_item->>'qty')::numeric, 0);

    v_harga := NULL;
    SELECT h.harga_beli INTO v_harga FROM bahan_baku_harga h WHERE h.bahan_baku_id = v_bahan_id;

    v_kategori := NULL;
    SELECT COALESCE(NULLIF(b.kategori, ''), 'LAIN-LAIN') INTO v_kategori
      FROM bahan_baku b WHERE b.id = v_bahan_id;
    v_kategori := COALESCE(v_kategori, 'LAIN-LAIN');

    IF v_harga IS NULL THEN
      v_tanpa_harga := v_tanpa_harga || to_jsonb(v_bahan_id);
    ELSE
      v_subtotal := v_qty * v_harga;
      v_total    := v_total + v_subtotal;
      v_kategori_nilai := jsonb_set(
        v_kategori_nilai,
        ARRAY[v_kategori],
        to_jsonb(COALESCE((v_kategori_nilai->>v_kategori)::numeric, 0) + v_subtotal),
        true
      );
    END IF;
  END LOOP;

  RETURN jsonb_build_object(
    'total_nilai', v_total,
    'item_tanpa_harga', v_tanpa_harga,
    'kategori_nilai', v_kategori_nilai
  );
END;
$$;

REVOKE EXECUTE ON FUNCTION public.estimasi_nilai_keranjang(jsonb) FROM PUBLIC, anon;
GRANT  EXECUTE ON FUNCTION public.estimasi_nilai_keranjang(jsonb) TO authenticated, service_role;

-- ---------------------------------------------------------------------------
-- 2. Status budget outlet, dibatasi outlet yang boleh diakses pemanggil
--    (cermin assertOutletAccessible). Bentuk hasil = get_outlet_budget_status.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.get_outlet_budget_status_scoped(p_outlet_id uuid)
RETURNS TABLE (
  nominal      NUMERIC,
  period_type  TEXT,
  period_start DATE,
  period_end   DATE,
  terpakai     NUMERIC,
  sisa         NUMERIC,
  has_config   BOOLEAN,
  custom_days  INT
)
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF NOT (p_outlet_id IN (SELECT accessible_outlet_ids())) THEN
    RAISE EXCEPTION 'Forbidden: tidak punya akses ke outlet %', p_outlet_id;
  END IF;
  RETURN QUERY SELECT * FROM public.get_outlet_budget_status(p_outlet_id);
END;
$$;

REVOKE EXECUTE ON FUNCTION public.get_outlet_budget_status_scoped(uuid) FROM PUBLIC, anon;
GRANT  EXECUTE ON FUNCTION public.get_outlet_budget_status_scoped(uuid) TO authenticated, service_role;

-- ---------------------------------------------------------------------------
-- 3. Ajukan top-up, dibatasi outlet yang boleh diakses pemanggil
--    (cermin requestBudgetTopupAction). Mengembalikan id request yang dibuat.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.request_budget_topup_scoped(
  p_outlet_id        uuid,
  p_requested_amount numeric,
  p_period_category  text
)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_req public.outlet_budget_topup_requests;
BEGIN
  IF NOT (p_outlet_id IN (SELECT accessible_outlet_ids())) THEN
    RAISE EXCEPTION 'Forbidden: tidak punya akses ke outlet %', p_outlet_id;
  END IF;
  IF p_period_category NOT IN ('weekday', 'weekend') THEN
    RAISE EXCEPTION 'Kategori periode tidak valid: %', p_period_category;
  END IF;
  IF p_requested_amount IS NULL OR p_requested_amount <= 0 THEN
    RAISE EXCEPTION 'Nominal tidak valid';
  END IF;
  v_req := public.request_budget_topup_svc(p_outlet_id, p_requested_amount, p_period_category);
  RETURN v_req.id;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.request_budget_topup_scoped(uuid, numeric, text) FROM PUBLIC, anon;
GRANT  EXECUTE ON FUNCTION public.request_budget_topup_scoped(uuid, numeric, text) TO authenticated, service_role;
