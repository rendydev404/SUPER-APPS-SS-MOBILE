-- FIX: `save_global_attendance_config` menimpa SEMUA outlet, bukan cuma aturan pusat.
--
-- Versi lama, setelah menulis global_settings, melakukan ini:
--
--     insert into public.outlet_attendance_config(...)
--     select o.id, p_jam_masuk, ..., 'auto', now()
--     from public.outlets o
--     on conflict (outlet_id) do update set ...;
--
-- Artinya setiap kali admin menekan "Simpan Pengaturan" di app native:
--   1. semua jadwal khusus per outlet ditimpa nilai pusat, dan
--   2. `outlet_attendance_config` terisi satu baris untuk SETIAP outlet.
-- Itu sebabnya daftar jadwal khusus selalu muncul lagi sesaat setelah direset, dan
-- kenapa tombol tambah bilang "semua outlet sudah punya jadwal khusus".
--
-- Model yang benar (sama dengan web `saveGlobalConfig`): aturan pusat cukup disimpan di
-- `global_settings`; `outlet_attendance_config` HANYA berisi pengecualian. `submit_attendance`
-- sudah membaca tabel itu dulu lalu fallback ke global, jadi outlet tanpa baris otomatis
-- ikut aturan pusat — tidak perlu di-fan-out.
--
-- Signature & return type tidak berubah, jadi CREATE OR REPLACE aman (tidak perlu DROP,
-- GRANT yang ada tetap berlaku). Hanya app Android yang memanggil RPC ini; web memakai
-- server action dengan tulis-tabel langsung, jadi web tidak terpengaruh.

create or replace function public.save_global_attendance_config(
  p_jam_masuk time without time zone,
  p_jam_keluar time without time zone,
  p_toleransi_menit integer,
  p_radius_m integer
)
returns jsonb
language plpgsql
security definer
set search_path to 'public'
as $function$
declare
  caller_role text;
  existing_mode text;
begin
  select os.role
    into caller_role
  from public.outlet_staff os
  where os.id = auth.uid()
    and os.status = 'active';

  if caller_role is null
     or caller_role <> all (array['admin'::text, 'admin_hr'::text, 'regional_manager'::text])
  then
    raise exception 'Akses ditolak: hanya admin, admin_hr, dan regional_manager yang dapat mengubah aturan pusat.';
  end if;

  if p_jam_masuk is null or p_jam_keluar is null then
    raise exception 'Jam masuk dan jam keluar wajib diisi.';
  end if;

  if p_toleransi_menit < 0 then
    raise exception 'Toleransi tidak boleh kurang dari 0 menit.';
  end if;

  if p_radius_m <= 0 then
    raise exception 'Radius geofence harus lebih besar dari 0 meter.';
  end if;

  -- Pertahankan mode yang sudah ada. Versi lama menulis 'auto' keras di sini, sehingga
  -- menyimpan dari HP diam-diam mengembalikan mode "manual" yang di-set lewat web.
  -- Panel pusat di Android memang belum punya field mode, jadi jangan diubah dari sini.
  select coalesce(gs.value->>'absen_window_mode', 'auto')
    into existing_mode
  from public.global_settings gs
  where gs.key = 'global_attendance_config';

  insert into public.global_settings(key, value)
  values (
    'global_attendance_config',
    jsonb_build_object(
      'jam_masuk', to_char(p_jam_masuk, 'HH24:MI'),
      'jam_keluar', to_char(p_jam_keluar, 'HH24:MI'),
      'toleransi_menit', p_toleransi_menit,
      'radius_m', p_radius_m,
      'absen_window_mode', coalesce(existing_mode, 'auto')
    )
  )
  on conflict (key) do update
    set value = excluded.value,
        updated_at = now();

  -- Sengaja TIDAK menyentuh outlet_attendance_config sama sekali.
  return jsonb_build_object(
    'success', true,
    'scope', 'global_only'
  );
end;
$function$;
