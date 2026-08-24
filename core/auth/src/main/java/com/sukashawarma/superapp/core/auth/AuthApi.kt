package com.sukashawarma.superapp.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

data class SignInPayload(val email: String, val password: String)
data class RefreshTokenPayload(@com.google.gson.annotations.SerializedName("refresh_token") val refreshToken: String)
data class AuthUserDto(val id: String, val email: String?)
data class AuthTokenResponse(
    val access_token: String,
    val refresh_token: String,
    val expires_in: Int,
    val token_type: String,
    val user: AuthUserDto
)
data class UpdatePasswordPayload(val password: String)

interface AuthApi {
    @POST("auth/v1/token")
    suspend fun signInWithPassword(
        @Query("grant_type") grantType: String = "password",
        @Body payload: SignInPayload
    ): Response<AuthTokenResponse>

    @POST("auth/v1/token")
    suspend fun refreshSession(
        @Query("grant_type") grantType: String = "refresh_token",
        @Body payload: RefreshTokenPayload
    ): Response<AuthTokenResponse>

    /** Ganti password akun sendiri — butuh access_token user aktif (bukan anon key),
     *  makanya Authorization dioverride manual di sini alih-alih lewat interceptor
     *  default (yang fallback ke anon key kalau belum login, tidak relevan di sini
     *  karena endpoint ini hanya dipanggil saat sudah login). */
    @PUT("auth/v1/user")
    suspend fun updatePassword(
        @Header("Authorization") bearerToken: String,
        @Body payload: UpdatePasswordPayload
    ): Response<AuthUserDto>
}
