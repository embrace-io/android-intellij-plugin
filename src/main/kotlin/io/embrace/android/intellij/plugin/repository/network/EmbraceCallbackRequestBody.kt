package io.embrace.android.intellij.plugin.repository.network

import com.google.gson.annotations.SerializedName

data class EmbraceCallbackRequestBody(
    @SerializedName("app_id")
    val appId: String?,

    @SerializedName("token")
    val token: String?,

    @SerializedName("session_id")
    val sessionId: String?,

    @SerializedName("external_user_id")
    val externalUserId: String,

    @SerializedName("project_name")
    val projectName: String,
)