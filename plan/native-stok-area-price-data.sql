-- Read-only parity endpoint for native Area Manager price board.
-- No changes to existing web policies or price writes.
CREATE OR REPLACE FUNCTION public.native_stok_price_data(p_days integer DEFAULT 30, p_bahan_id uuid DEFAULT NULL)
RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public
AS $function$
DECLARE result jsonb;
BEGIN
  IF NOT EXISTS (SELECT 1 FROM public.outlet_staff WHERE id = auth.uid() AND status = 'active'
    AND role IN ('area_manager', 'leader', 'regional_manager', 'spv')) THEN
    RAISE EXCEPTION 'Akses modul Area Manager ditolak' USING ERRCODE = '42501';
  END IF;
  IF p_days IS NOT NULL AND p_days NOT IN (7, 30, 90) THEN
    RAISE EXCEPTION 'Periode tidak valid' USING ERRCODE = '22023';
  END IF;
  SELECT jsonb_build_object(
    'materials', COALESCE((SELECT jsonb_agg(to_jsonb(b) ORDER BY b.nama, b.id)
      FROM (SELECT id, nama, satuan, kategori, kategori_core FROM public.bahan_baku
            WHERE is_active = true AND (p_bahan_id IS NULL OR id = p_bahan_id)) b), '[]'::jsonb),
    'prices', COALESCE((SELECT jsonb_agg(to_jsonb(h))
      FROM (SELECT bahan_baku_id, harga_beli, harga_beli_display FROM public.bahan_baku_harga
            WHERE p_bahan_id IS NULL OR bahan_baku_id = p_bahan_id) h), '[]'::jsonb),
    'purchases', COALESCE((SELECT jsonb_agg(jsonb_build_object(
        'id', i.id, 'bahan_baku_id', i.bahan_baku_id, 'qty_terima', i.qty_terima,
        'harga_terima', i.harga_terima, 'subtotal', i.subtotal,
        'purchase_order', jsonb_build_object('id', p.id, 'nomor_po', p.nomor_po,
          'tanggal_po', p.tanggal_po, 'supplier_nama', p.supplier_nama, 'status', p.status))
        ORDER BY p.tanggal_po DESC, i.id)
      FROM public.purchase_order_item i JOIN public.purchase_order p ON p.id = i.purchase_order_id
      WHERE p.status IN ('diterima_lengkap', 'sebagian_diterima') AND i.harga_terima > 0
        AND (p_bahan_id IS NULL OR i.bahan_baku_id = p_bahan_id)
        AND (p_days IS NULL OR p.tanggal_po >= ((now() AT TIME ZONE 'UTC')::date - p_days))), '[]'::jsonb)
  ) INTO result;
  RETURN result;
END;
$function$;
REVOKE ALL ON FUNCTION public.native_stok_price_data(integer, uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.native_stok_price_data(integer, uuid) TO authenticated;
