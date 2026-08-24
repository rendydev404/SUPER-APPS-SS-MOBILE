package com.sukashawarma.superapp.data.remote

import com.sukashawarma.superapp.core.network.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object SupabaseClient {
    var onRefreshNeeded: (suspend () -> Boolean)? = null
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
        val path = response.request.url.encodedPath
        if (path.contains("auth/v1/token") || response.request.header(RETRY_HEADER) != null) return@Authenticator null
        val refreshed = kotlinx.coroutines.runBlocking { onRefreshNeeded?.invoke() ?: false }
        if (!refreshed) return@Authenticator null
        response.request.newBuilder().header(RETRY_HEADER, "1").build()
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




