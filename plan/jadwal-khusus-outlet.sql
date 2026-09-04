-- Jadwal khusus (pengecualian) jam kerja per outlet — dipakai layar Pengaturan
-- Absensi di app native. Cermin logika web `apps/absensi/src/app/dashboard/pengaturan`
-- (server action `saveOutletException` / `deleteOutletException` / `deleteAllExceptions`).
--
-- Kenapa lewat RPC, bukan upsert tabel langsung seperti di web?
-- Web memakai service-role key di server action, jadi RLS dilewati. App native hanya
-- punya JWT user, sementara `outlet_attendance_config` di-RLS ketat: SELECT hanya untuk
-- outlet sendiri dan sama sekali tidak ada policy INSERT/DELETE (lihat migrasi web
-- `20260610000300_m1_attendance_rls.sql`). Tiga fungsi SECURITY DEFINER di bawah ini
-- yang menggantikan peran service-role tersebut, dengan cek role yang sama.
--
-- Jalankan sekali di Supabase SQL Editor (project produksi absensi).

-- Gate role: cermin SETTINGS_ALLOWED_ROLES di web.
create or replace function public.assert_attendance_settings_admin()
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if not exists (
    select 1 from outlet_staff me
    where me.id = auth.uid()
      and me.status = 'active'
      and me.role in ('admin', 'admin_hr', 'regional_manager')
  ) then
    raise exception 'Akses ditolak: hanya admin, admin HR, dan regional manager yang boleh mengubah pengaturan absensi'
      using errcode = '42501';
  end if;
end;
$$;

-- Daftar seluruh pengecualian + nama outletnya. RLS `oac_read_own_outlet` membatasi
-- SELECT ke outlet sendiri, jadi admin tetap butuh fungsi ini untuk melihat semuanya.
create or replace function public.list_outlet_attendance_config()
returns table (
  outlet_id uuid,
  outlet_name text,
  jam_masuk text,
  jam_keluar text,
  toleransi_menit int,
  radius_m int,
  absen_window_mode text
)
language plpgsql
security definer
set search_path = public
as $$
begin
  perform assert_attendance_settings_admin();
  return query
    select
      c.outlet_id,
      o.name::text,
      left(c.jam_masuk::text, 5),
      left(c.jam_keluar::text, 5),
      c.toleransi_menit,
      c.radius_m,
      coalesce(c.absen_window_mode, 'auto')
    from outlet_attendance_config c
    join outlets o on o.id = c.outlet_id
    order by o.name;
end;
$$;

create or replace function public.save_outlet_attendance_config(
  p_outlet_id uuid,
  p_jam_masuk text,
  p_jam_keluar text,
  p_toleransi_menit int,
  p_radius_m int,
  p_absen_window_mode text default 'auto'
)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  perform assert_attendance_settings_admin();

  if p_outlet_id is null then
    raise exception 'Outlet wajib dipilih' using errcode = '22023';
  end if;
  if coalesce(p_absen_window_mode, 'auto') not in ('auto', 'manual') then
    raise exception 'Mode absensi tidak valid' using errcode = '22023';
  end if;

  insert into outlet_attendance_config (
    outlet_id, jam_masuk, jam_keluar, toleransi_menit, radius_m, absen_window_mode
  ) values (
    p_outlet_id,
    p_jam_masuk::time,
    p_jam_keluar::time,
    p_toleransi_menit,
    p_radius_m,
    coalesce(p_absen_window_mode, 'auto')
  )
  on conflict (outlet_id) do update set
    jam_masuk = excluded.jam_masuk,
    jam_keluar = excluded.jam_keluar,
    toleransi_menit = excluded.toleransi_menit,
    radius_m = excluded.radius_m,
    absen_window_mode = excluded.absen_window_mode;
end;
$$;

-- DROP dulu, bukan CREATE OR REPLACE: versi pertama fungsi ini punya
-- `p_outlet_id uuid default null`, dan Postgres menolak replace yang menghapus default
-- parameter (42P13 "cannot remove parameter defaults from existing function").
-- Aman diulang; GRANT-nya dipasang lagi di bagian bawah file.
drop function if exists public.delete_outlet_attendance_config(uuid);

create or replace function public.delete_outlet_attendance_config(p_outlet_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  perform assert_attendance_settings_admin();
  if p_outlet_id is null then
    raise exception 'Outlet wajib dipilih' using errcode = '22023';
  end if;
  delete from outlet_attendance_config where outlet_id = p_outlet_id;
end;
$$;

-- Reset seluruh pengecualian (semua outlet balik ke aturan pusat), cermin
-- `deleteAllExceptions` di web.
--
-- Sengaja fungsi TERPISAH tanpa argumen, bukan `delete_outlet_attendance_config(null)`:
-- mengirim `{"p_outlet_id": null}` ke PostgREST bikin argumennya di-cast sebagai teks
-- ke uuid dan requestnya ditolak 400 sebelum fungsinya sempat jalan.
create or replace function public.reset_outlet_attendance_config()
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  perform assert_attendance_settings_admin();
  -- `where outlet_id is not null` bukan hiasan: Supabase mengaktifkan pg_safeupdate,
  -- yang menolak DELETE tanpa WHERE ("DELETE requires a WHERE clause"). outlet_id
  -- adalah primary key (NOT NULL), jadi klausa ini tetap kena semua baris.
  delete from outlet_attendance_config where outlet_id is not null;
end;
$$;

revoke all on function public.assert_attendance_settings_admin() from public, anon;
revoke all on function public.list_outlet_attendance_config() from public, anon;
revoke all on function public.save_outlet_attendance_config(uuid, text, text, int, int, text) from public, anon;
revoke all on function public.delete_outlet_attendance_config(uuid) from public, anon;
revoke all on function public.reset_outlet_attendance_config() from public, anon;

grant execute on function public.list_outlet_attendance_config() to authenticated;
grant execute on function public.save_outlet_attendance_config(uuid, text, text, int, int, text) to authenticated;
grant execute on function public.delete_outlet_attendance_config(uuid) to authenticated;
grant execute on function public.reset_outlet_attendance_config() to authenticated;

-- DOWN:
-- drop function if exists public.reset_outlet_attendance_config();
-- drop function if exists public.delete_outlet_attendance_config(uuid);
-- drop function if exists public.save_outlet_attendance_config(uuid, text, text, int, int, text);
-- drop function if exists public.list_outlet_attendance_config();
-- drop function if exists public.assert_attendance_settings_admin();
