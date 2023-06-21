package io.embrace.android.intellij.plugin.data

internal enum class StartMethodStatus {
    START_ADDED_SUCCESSFULLY,
    START_ALREADY_ADDED,
    APPLICATION_CLASS_NOT_FOUND,
    APPLICATION_CLASS_NOT_ON_CREATE,
    ERROR
}