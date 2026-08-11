package com.showcasevault.nextelis.network

import com.showcasevault.nextelis.NexTelisApplication
import com.showcasevault.nextelis.session.SessionStore
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit instance for the NexTelis control-plane API.
 *
 * The base URL is whatever server the user entered on the "Connect to
 * server" screen (see ServerSetupActivity), stored in SessionStore — this
 * makes a single APK usable against any NexTelis deployment. There is no
 * build-time default: [currentBaseUrl] throws if no server is configured,
 * which should be unreachable since AppFlow always routes there first.
 *
 * The client is rebuilt lazily whenever the configured base URL changes —
 * call [reset] after saving a new server config so the next [api] access
 * picks it up.
 */
object NexTelisApiClient {

    private var cached: NexTelisApi? = null
    private var cachedBaseUrl: String? = null

    val api: NexTelisApi
        get() {
            val baseUrl = currentBaseUrl()
            val existing = cached
            if (existing != null && cachedBaseUrl == baseUrl) return existing
            return buildRetrofit(baseUrl).create(NexTelisApi::class.java).also {
                cached = it
                cachedBaseUrl = baseUrl
            }
        }

    /** Forces the next [api] access to rebuild the client against the current server config. */
    fun reset() {
        cached = null
        cachedBaseUrl = null
    }

    private fun currentBaseUrl(): String {
        val context = NexTelisApplication.instance
        return SessionStore.getServerBaseUrl(context)
            ?: error("No NexTelis server configured — set one via the Connect to Server screen.")
    }

    private fun buildRetrofit(baseUrl: String): Retrofit {
        val moshi = Moshi.Builder()
            .add(UuidAdapter())
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
            .baseUrl(baseUrl)
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
