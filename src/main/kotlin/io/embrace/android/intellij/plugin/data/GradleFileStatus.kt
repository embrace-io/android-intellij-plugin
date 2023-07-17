package io.embrace.android.intellij.plugin.data

internal enum class GradleFileStatus {
    ADDED_SUCCESSFULLY,
    FILE_NOT_FOUND,
    SWAZZLER_ALREADY_ADDED,
    DEPENDENCIES_BLOCK_NOT_FOUND,
    PLUGINS_BLOCK_NOT_FOUND,
    ERROR
}