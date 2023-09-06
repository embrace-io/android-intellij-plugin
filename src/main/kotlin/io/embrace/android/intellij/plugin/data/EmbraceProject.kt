package io.embrace.android.intellij.plugin.data


internal data class EmbraceProject(
    val appId: String,
    val token: String,
    val sessionId: String?,
    val externalUserId: String
)
