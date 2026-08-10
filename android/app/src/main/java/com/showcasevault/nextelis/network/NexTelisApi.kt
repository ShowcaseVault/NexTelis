package com.showcasevault.nextelis.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.UUID

data class UserCreateRequest(
    val email: String,
    val display_name: String
)

data class UserRead(
    val id: UUID,
    val email: String,
    val display_name: String,
    val is_active: Boolean,
    val created_at: String
)

data class UserWithClaimCode(
    val user: UserRead,
    val claim_code: String,
    val claim_code_expires_at: String
)

data class DeviceClaimRequest(
    val claim_code: String,
    val device_name: String,
    val push_token: String? = null
)

data class DeviceRead(
    val id: UUID,
    val name: String,
    val is_active: Boolean,
    val user_id: UUID,
    val created_at: String
)

data class DeviceClaimResponse(
    val device: DeviceRead,
    val device_token: String
)

data class NumberRead(
    val id: UUID,
    val value: String,
    val is_active: Boolean,
    val user_id: UUID,
    val created_at: String,
    val sip_password: String
)

// Mirrors backend/api/v1/routes (users, devices, numbers) — see docs/ARCHITECTURE.md for the control-plane contract.
interface NexTelisApi {

    @POST("api/v1/users")
    suspend fun registerUser(@Body body: UserCreateRequest): UserWithClaimCode

    @POST("api/v1/devices/claim")
    suspend fun claimDevice(@Body body: DeviceClaimRequest): DeviceClaimResponse

    @POST("api/v1/users/{userId}/number")
    suspend fun assignNumber(@Path("userId") userId: UUID): NumberRead

    @GET("api/v1/users/{userId}/number")
    suspend fun getNumber(@Path("userId") userId: UUID): NumberRead
}
