package com.sukashawarma.superapp.data.remote

import com.sukashawarma.superapp.core.network.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object SupabaseClient {
    /** Menerima access token yang baru saja ditolak server (null bila request dikirim
     *  dengan anon key); mengembalikan true bila sesi sudah punya token yang bisa dicoba. */
    var onRefreshNeeded: (suspend (tokenGagal: String?) -> Boolean)? = null
    const val BASE_URL = "${BuildConfig.SUPABASE_URL}/"
    private const val ANON_KEY = BuildConfig.SUPABASE_ANON_KEY
    private const val HOST = "khpkoreaaucvyqfhynfq.supabase.co"

    // apikey wajib untuk PostgREST/GoTrue; Authorization = access token user aktif
    // supaya RLS berlaku sama seperti web. Fallback ke anon key pra-login.
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        if (original.url.host != HOST) return@Interceptor chain.proceed(original)

        val bearer = SessionTokenHolder.accessToken ?: ANON_KEY
        val requestBuilder = original.newBuilder()
            .header("apikey", ANON_KEY)
            .header("Authorization", "Bearer $bearer")
            .header("User-Agent", "SukaSuperapp/1.0 (Android ${android.os.Build.VERSION.SDK_INT})")

        if (original.header("Content-Type") == null) {
            requestBuilder.header("Content-Type", "application/json")
        }
        chain.proceed(requestBuilder.build())
    }

    // 401 = token kedaluwarsa. Refresh sekali lalu ulangi request.
    private val tokenAuthenticator = okhttp3.Authenticator { _, response ->
        val request = response.request
        if (request.url.encodedPath.contains("auth/v1/token") || request.header(RETRY_HEADER) != null) {
            return@Authenticator null
        }
        val tokenGagal = request.header("Authorization")?.removePrefix("Bearer ")?.takeIf { it != ANON_KEY }
        // Request anonim tanpa sesi sama sekali: tidak ada yang bisa di-refresh.
        if (tokenGagal == null && SessionTokenHolder.refreshToken == null) return@Authenticator null

        val siap = kotlinx.coroutines.runBlocking { onRefreshNeeded?.invoke(tokenGagal) ?: false }
        val tokenBaru = SessionTokenHolder.accessToken
        // Mengulang dengan token yang sama pasti 401 lagi — hanya menambah beban server.
        if (!siap || tokenBaru == null || tokenBaru == tokenGagal) return@Authenticator null

        // Header wajib ditulis ulang di sini. authInterceptor adalah application
        // interceptor dan OkHttp tidak menjalankannya lagi untuk request susulan
        // authenticator: dulu retry membawa token lama yang baru saja ditolak, jadi
        // refresh yang berhasil pun tetap berakhir 401.
        request.newBuilder()
            .header("Authorization", "Bearer $tokenBaru")
            .header(RETRY_HEADER, "1")
            .build()
    }

    private const val RETRY_HEADER = "X-Token-Retry"

    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    
}




