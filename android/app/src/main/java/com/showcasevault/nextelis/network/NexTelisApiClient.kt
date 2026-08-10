package com.showcasevault.nextelis.network

import com.showcasevault.nextelis.BuildConfig
import com.showcasevault.nextelis.NexTelisApplication
import com.showcasevault.nextelis.session.SessionStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Single Retrofit instance for the NexTelis control-plane API.
 *
 * BASE_URL comes from BuildConfig.API_BASE_URL, which is generated at build
 * time from android/local.properties (nextelis.api.baseUrl) — never
 * hardcoded here. See docs/ARCHITECTURE.md §8 (Local Development
 * Architecture) and android/local.properties for how to point this at your
 * own server.
 */
object NexTelisApiClient {

    val api: NexTelisApi by lazy { buildRetrofit().create(NexTelisApi::class.java) }

    private fun buildRetrofit(): Retrofit {
        val moshi = Moshi.Builder()
            .add(UuidAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(::attachDeviceToken)
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    // Endpoints like /users/{id}/number require the caller's device_token —
    // see backend/api/v1/routes/numbers.py's _require_own_user check.
    private fun attachDeviceToken(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val token = SessionStore.getDeviceToken(NexTelisApplication.instance)
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
