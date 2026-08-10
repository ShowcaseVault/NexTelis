package com.showcasevault.nextelis.network

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.util.UUID

/** Moshi has no built-in adapter for java.util.UUID — the backend sends/expects plain strings. */
class UuidAdapter {

    @FromJson
    fun fromJson(value: String): UUID = UUID.fromString(value)

    @ToJson
    fun toJson(value: UUID): String = value.toString()
}
