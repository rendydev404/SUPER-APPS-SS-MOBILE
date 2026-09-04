-- Fitur pengajuan top-up saldo outlet — bagian backend.
-- Jalankan di Supabase SQL Editor.
--
-- Diambil dari migration web 20260820110001_outlet_budget_topup_ledger.sql, TETAPI
-- HANYA bagian top-up-nya. Migration asli juga:
--   - DROP COLUMN period_type & custom_days pada outlet_budget_config, dan
--   - mengganti get_outlet_budget_status menjadi berbasis outlet_balance, dan
--   - mengganti approve_permintaan_svc agar mendebit saldo.
-- Ketiganya SENGAJA TIDAK diikutkan. Produksi memakai versi lama
-- (20260819150000): periode custom, terpakai = SUM(qty_disetujui * harga_snapshot).
-- Kalau ketiganya ikut dijalankan, badge budget seluruh outlet di web berubah
-- seketika menjadi "sisa = outlet_balance" yang isinya 0 — semua outlet langsung
-- tampak merah/habis. Itu perubahan berisiko yang harus diputuskan terpisah.
--
-- KONSEKUENSI YANG HARUS DISADARI: karena get_outlet_budget_status yang aktif tidak
-- membaca outlet_balance, top-up yang disetujui TERCATAT (saldo & buku kas terisi)
-- tetapi BELUM mengubah angka sisa budget di layar. Alur pengajuan-persetujuannya
-- berjalan penuh; efek ke angka budget menyusul bila nanti fungsi budget diganti.

-- ---------------------------------------------------------------------------
-- 1. Dompet outlet
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.outlet_balance (
  outlet_id       UUID PRIMARY KEY REFERENCES public.outlets(id) ON DELETE CASCADE,
  current_balance NUMERIC NOT NULL DEFAULT 0,
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.outlet_balance ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS ob_select ON public.outlet_balance;
CREATE POLICY ob_select ON public.outlet_balance FOR SELECT TO authenticated
  USING (outlet_id IN (SELECT accessible_outlet_ids()));

-- ---------------------------------------------------------------------------
-- 2. Pengajuan top-up
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.outlet_budget_topup_requests (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  outlet_id           UUID NOT NULL REFERENCES public.outlets(id) ON DELETE CASCADE,
  requested_amount    NUMERIC NOT NULL CHECK (requested_amount > 0),
  period_category     TEXT NOT NULL CHECK (period_category IN ('weekday', 'weekend')),
  status              TEXT NOT NULL DEFAULT 'pending_am'
                        CHECK (status IN ('pending_am', 'pending_finance', 'approved', 'rejected')),
  created_by          UUID REFERENCES public.outlet_staff(id),
  am_approved_by      UUID REFERENCES public.outlet_staff(id),
  finance_approved_by UUID REFERENCES public.outlet_staff(id),
  notes               TEXT,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_topup_outlet_created
  ON public.outlet_budget_topup_requests(outlet_id, created_at DESC);

ALTER TABLE public.outlet_budget_topup_requests ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS ob_topup_select ON public.outlet_budget_topup_requests;
CREATE POLICY ob_topup_select ON public.outlet_budget_topup_requests FOR SELECT TO authenticated
  USING (outlet_id IN (SELECT accessible_outlet_ids()));

DROP POLICY IF EXISTS ob_topup_insert ON public.outlet_budget_topup_requests;
CREATE POLICY ob_topup_insert ON public.outlet_budget_topup_requests FOR INSERT TO authenticated
  WITH CHECK (outlet_id IN (SELECT accessible_outlet_ids()));

-- ---------------------------------------------------------------------------
-- 3. Buku kas saldo
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.outlet_balance_ledger (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  outlet_id        UUID NOT NULL REFERENCES public.outlets(id) ON DELETE CASCADE,
  transaction_type TEXT NOT NULL CHECK (transaction_type IN ('TOP_UP', 'MATERIAL_PURCHASE')),
  reference_id     UUID NOT NULL,
  credit           NUMERIC NOT NULL DEFAULT 0 CHECK (credit >= 0),
  debit            NUMERIC NOT NULL DEFAULT 0 CHECK (debit >= 0),
  balance_after    NUMERIC NOT NULL,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.outlet_balance_ledger ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS ob_ledger_select ON public.outlet_balance_ledger;
CREATE POLICY ob_ledger_select ON public.outlet_balance_ledger FOR SELECT TO authenticated
  USING (outlet_id IN (SELECT accessible_outlet_ids()));

-- ---------------------------------------------------------------------------
-- 3b. Realtime — web (`useOutletTopupRequests`) berlangganan perubahan ketiga
--     tabel ini. Tanpa ini daftarnya tetap benar, hanya tidak menyegar sendiri.
--     Dijaga IF NOT EXISTS supaya aman diulang.
-- ---------------------------------------------------------------------------
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = 'outlet_balance'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.outlet_balance;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = 'outlet_budget_topup_requests'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.outlet_budget_topup_requests;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = 'outlet_balance_ledger'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.outlet_balance_ledger;
  END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 4. RPC layanan (sama persis dengan migration web) — hanya untuk service_role.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.request_budget_topup_svc(
  p_outlet_id        UUID,
  p_requested_amount NUMERIC,
  p_period_category  TEXT
)
RETURNS public.outlet_budget_topup_requests
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_nominal     NUMERIC;
  v_sisa        NUMERIC;
  v_max_request NUMERIC;
  v_request     public.outlet_budget_topup_requests;
BEGIN
  SELECT nominal INTO v_nominal FROM outlet_budget_config WHERE outlet_id = p_outlet_id;
  IF v_nominal IS NULL THEN
    RAISE EXCEPTION 'Outlet belum memiliki konfigurasi budget (plafon)';
  END IF;

  SELECT current_balance INTO v_sisa FROM outlet_balance WHERE outlet_id = p_outlet_id;
  IF v_sisa IS NULL THEN
    v_sisa := 0;
  END IF;

  v_max_request := v_nominal - v_sisa;

  IF p_requested_amount > v_max_request THEN
    RAISE EXCEPTION 'Jumlah request melebihi sisa plafon. Maksimal yang bisa diajukan adalah %', v_max_request;
  END IF;

  INSERT INTO public.outlet_budget_topup_requests (outlet_id, requested_amount, period_category, status, created_by)
  VALUES (p_outlet_id, p_requested_amount, p_period_category, 'pending_am', auth.uid())
  RETURNING * INTO v_request;

  RETURN v_request;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.request_budget_topup_svc(uuid, numeric, text) FROM PUBLIC, anon, authenticated;
GRANT  EXECUTE ON FUNCTION public.request_budget_topup_svc(uuid, numeric, text) TO service_role;

CREATE OR REPLACE FUNCTION public.approve_budget_topup_svc(
  p_request_id UUID,
  p_action     TEXT,
  p_notes      TEXT DEFAULT NULL
)
RETURNS public.outlet_budget_topup_requests
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_request     public.outlet_budget_topup_requests;
  v_new_balance NUMERIC;
BEGIN
  SELECT * INTO v_request FROM outlet_budget_topup_requests WHERE id = p_request_id FOR UPDATE;

  IF v_request.id IS NULL THEN
    RAISE EXCEPTION 'Request top-up tidak ditemukan';
  END IF;

  IF v_request.status IN ('approved', 'rejected') THEN
    RAISE EXCEPTION 'Request sudah dalam status %', v_request.status;
  END IF;

  IF p_action = 'reject' THEN
    UPDATE outlet_budget_topup_requests
    SET status = 'rejected', notes = p_notes, updated_at = NOW()
    WHERE id = p_request_id RETURNING * INTO v_request;
    RETURN v_request;
  END IF;

  IF p_action = 'approve_am' THEN
    IF v_request.status != 'pending_am' THEN
      RAISE EXCEPTION 'Hanya bisa approve_am jika status pending_am';
    END IF;
    UPDATE outlet_budget_topup_requests
    SET status = 'pending_finance', am_approved_by = auth.uid(), notes = COALESCE(p_notes, notes), updated_at = NOW()
    WHERE id = p_request_id RETURNING * INTO v_request;
    RETURN v_request;
  END IF;

  IF p_action = 'approve_finance' THEN
    IF v_request.status != 'pending_finance' THEN
      RAISE EXCEPTION 'Hanya bisa approve_finance jika status pending_finance';
    END IF;

    UPDATE outlet_budget_topup_requests
    SET status = 'approved', finance_approved_by = auth.uid(), notes = COALESCE(p_notes, notes), updated_at = NOW()
    WHERE id = p_request_id RETURNING * INTO v_request;

    INSERT INTO outlet_balance (outlet_id, current_balance, updated_at)
    VALUES (v_request.outlet_id, v_request.requested_amount, NOW())
    ON CONFLICT (outlet_id)
    DO UPDATE SET current_balance = outlet_balance.current_balance + v_request.requested_amount, updated_at = NOW()
    RETURNING current_balance INTO v_new_balance;

    INSERT INTO outlet_balance_ledger (outlet_id, transaction_type, reference_id, credit, debit, balance_after)
    VALUES (v_request.outlet_id, 'TOP_UP', v_request.id, v_request.requested_amount, 0, v_new_balance);

    RETURN v_request;
  END IF;

  RAISE EXCEPTION 'Action tidak valid: %', p_action;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.approve_budget_topup_svc(uuid, text, text) FROM PUBLIC, anon, authenticated;
GRANT  EXECUTE ON FUNCTION public.approve_budget_topup_svc(uuid, text, text) TO service_role;

-- ---------------------------------------------------------------------------
-- 5. Pembungkus berpagar untuk klien ber-JWT user (aplikasi native).
--    Web memagari lewat Server Action; native tidak punya lapisan server, jadi
--    pagarnya dipasang di sini. Matriks role menyalin OutletTopUpRequests.tsx:
--      AM      : admin, owner, developer
--      Finance : admin_finance, owner, developer
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.request_budget_topup_scoped(
  p_outlet_id        UUID,
  p_requested_amount NUMERIC,
  p_period_category  TEXT
)
RETURNS UUID
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
  IF NOT EXISTS (SELECT 1 FROM outlet_staff WHERE id = auth.uid() AND status = 'active') THEN
    RAISE EXCEPTION 'Forbidden: akun tidak aktif';
  END IF;

  v_req := public.request_budget_topup_svc(p_outlet_id, p_requested_amount, p_period_category);
  RETURN v_req.id;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.request_budget_topup_scoped(uuid, numeric, text) FROM PUBLIC, anon;
GRANT  EXECUTE ON FUNCTION public.request_budget_topup_scoped(uuid, numeric, text) TO authenticated, service_role;

CREATE OR REPLACE FUNCTION public.approve_budget_topup_scoped(
  p_request_id UUID,
  p_action     TEXT,
  p_notes      TEXT DEFAULT NULL
)
RETURNS TEXT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_role    TEXT;
  v_outlet  UUID;
  v_request public.outlet_budget_topup_requests;
BEGIN
  SELECT role INTO v_role FROM outlet_staff WHERE id = auth.uid() AND status = 'active';
  IF v_role IS NULL THEN
    RAISE EXCEPTION 'Forbidden: akun tidak aktif';
  END IF;

  SELECT outlet_id INTO v_outlet FROM outlet_budget_topup_requests WHERE id = p_request_id;
  IF v_outlet IS NULL THEN
    RAISE EXCEPTION 'Request top-up tidak ditemukan';
  END IF;
  IF NOT (v_outlet IN (SELECT accessible_outlet_ids())) THEN
    RAISE EXCEPTION 'Forbidden: tidak punya akses ke outlet request ini';
  END IF;

  IF p_action = 'approve_am' AND v_role NOT IN ('admin', 'owner', 'developer') THEN
    RAISE EXCEPTION 'Forbidden: hanya admin, owner, atau developer yang boleh menyetujui tahap AM';
  END IF;

  IF p_action = 'approve_finance' AND v_role NOT IN ('admin_finance', 'owner', 'developer') THEN
    RAISE EXCEPTION 'Forbidden: hanya admin finance, owner, atau developer yang boleh menyetujui tahap Finance';
  END IF;

  -- Penolakan boleh dilakukan siapa pun yang berwenang pada salah satu tahap.
  IF p_action = 'reject'
     AND v_role NOT IN ('admin', 'owner', 'developer', 'admin_finance') THEN
    RAISE EXCEPTION 'Forbidden: Anda tidak berwenang menolak pengajuan top-up';
  END IF;

  v_request := public.approve_budget_topup_svc(p_request_id, p_action, p_notes);
  RETURN v_request.status;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.approve_budget_topup_scoped(uuid, text, text) FROM PUBLIC, anon;
GRANT  EXECUTE ON FUNCTION public.approve_budget_topup_scoped(uuid, text, text) TO authenticated, service_role;
