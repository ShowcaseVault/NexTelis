package com.showcasevault.nextelis.network

import com.showcasevault.nextelis.BuildConfig
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
            .add(KotlinJsonAdapterFactory())
            .build()

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val httpClient = OkHttpClient.Builder()
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
}
