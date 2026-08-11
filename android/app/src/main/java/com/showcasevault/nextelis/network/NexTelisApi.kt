package com.showcasevault.nextelis.network

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.UUID

@JsonClass(generateAdapter = true)
data class UserCreateRequest(
    val email: String,
    val display_name: String
)

@JsonClass(generateAdapter = true)
data class ClaimCodeRequest(
    val email: String,
    val recovery_code: String
)

@JsonClass(generateAdapter = true)
data class UserRead(
    val id: UUID,
    val email: String,
    val display_name: String,
    val is_active: Boolean,
    val created_at: String
)

@JsonClass(generateAdapter = true)
data class UserWithClaimCode(
    val user: UserRead,
    val claim_code: String,
    val claim_code_expires_at: String
)

@JsonClass(generateAdapter = true)
data class UserRegisteredResponse(
    val user: UserRead,
    val claim_code: String,
    val claim_code_expires_at: String,
    val recovery_code: String
)

@JsonClass(generateAdapter = true)
data class DeviceClaimRequest(
    val claim_code: String,
    val device_name: String,
    val push_token: String? = null
)

@JsonClass(generateAdapter = true)
data class DeviceRead(
    val id: UUID,
    val name: String,
    val is_active: Boolean,
    val user_id: UUID,
    val created_at: String
)

@JsonClass(generateAdapter = true)
data class DeviceClaimResponse(
    val device: DeviceRead,
    val device_token: String
)

@JsonClass(generateAdapter = true)
data class NumberRead(
    val id: UUID,
    val value: String,
    val is_active: Boolean,
    val user_id: UUID,
    val created_at: String,
    val sip_password: String
)

@JsonClass(generateAdapter = true)
data class NumberLookupResult(
    val value: String,
    val display_name: String
)

/**
 * Where and how to reach this deployment's SIP service. Separate from the
 * API's address because SIP/RTP are UDP and can't traverse an HTTP tunnel or
 * proxy the API may sit behind. sip_transport covers signalling only — media
 * is RTP over UDP regardless.
 */
@JsonClass(generateAdapter = true)
data class ServerInfo(
    val sip_host: String,
    val sip_port: Int,
    val sip_transport: String
)

// Mirrors backend/api/v1/routes (users, devices, numbers) — see docs/ARCHITECTURE.md for the control-plane contract.
interface NexTelisApi {

    @GET("api/v1/server/info")
    suspend fun getServerInfo(): ServerInfo

    @POST("api/v1/users")
    suspend fun registerUser(@Body body: UserCreateRequest): UserRegisteredResponse

    @POST("api/v1/users/claim-code")
    suspend fun reissueClaimCode(@Body body: ClaimCodeRequest): UserWithClaimCode

    @POST("api/v1/devices/claim")
    suspend fun claimDevice(@Body body: DeviceClaimRequest): DeviceClaimResponse

    @POST("api/v1/users/{userId}/number")
    suspend fun assignNumber(@Path("userId") userId: UUID): NumberRead

    @GET("api/v1/users/{userId}/number")
    suspend fun getNumber(@Path("userId") userId: UUID): NumberRead

    @GET("api/v1/numbers/{value}")
    suspend fun lookupNumber(@Path("value") value: String): NumberLookupResult
}
