package io.embrace.android.intellij.plugin.utils.extensions

import io.embrace.android.intellij.plugin.EmbraceStringResources

fun String.text(): String =  EmbraceStringResources.message(this)
