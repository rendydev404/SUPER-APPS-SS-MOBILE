package com.sukashawarma.superapp.domain.model

/** Cermin persis `packages/auth/src/types.ts` Role union (monorepo web). Jangan menyimpang —
 *  itu sudah pernah terjadi sebelumnya (native-superapp/Roles.kt vs web, lihat plan §2.5 X1). */
enum class Role(val value: String) {
    ADMIN("admin"),
    ADMIN_HR("admin_hr"),
    OWNER("owner"),
    SPV("spv"),
    REGIONAL_MANAGER("regional_manager"),
    LEADER("leader"),
    CREW("crew"),
    KIOSK("kiosk"),
    KITCHEN("kitchen"),
    MITRA("mitra"),
    STAFF_PUSAT("staff_pusat"),
    ADMIN_FINANCE("admin_finance"),
    AREA_MANAGER("area_manager"),
    PURCHASING("purchasing"),
    DEVELOPER("developer"),
    KORLAP("korlap"); // ada di DB & dipakai staff_outlets, belum resmi di union web (S7)

    companion object {
        fun from(value: String?): Role? = entries.find { it.value == value }
    }
}

/** Role yang punya menu SPV-tier di modul Absensi — cermin `isSPV` di dashboard/layout.tsx web. */
val SPV_TIER_ROLES = setOf(
    Role.ADMIN, Role.ADMIN_HR, Role.OWNER, Role.SPV, Role.LEADER,
    Role.REGIONAL_MANAGER, Role.AREA_MANAGER
)

/** Role yang boleh mengakses Pengaturan Absensi: admin, HR, dan regional manager. */
val ADMIN_OR_HR_ROLES = setOf(
    Role.ADMIN, Role.ADMIN_HR, Role.REGIONAL_MANAGER
)

/** Role yang boleh mengoperasikan halaman Enrollment — cermin SPV_ROLES di api/enroll/route.ts. */
val ENROLL_ALLOWED_ROLES = setOf(
    Role.SPV, Role.LEADER, Role.REGIONAL_MANAGER, Role.AREA_MANAGER,
    Role.ADMIN, Role.ADMIN_HR, Role.OWNER, Role.KITCHEN
)
