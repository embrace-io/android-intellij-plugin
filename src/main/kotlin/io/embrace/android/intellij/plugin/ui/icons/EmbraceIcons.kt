package io.embrace.android.intellij.plugin.ui.icons

import com.intellij.openapi.util.IconLoader

object EmbraceIcons {
    @JvmField
    val embrace_default_icon = IconLoader.getIcon("/icons/logo.svg", EmbraceIcons::class.java)

    @JvmField
    val toolwindowIcon = IconLoader.getIcon("/icons/toolwindowIcon.svg", EmbraceIcons::class.java)

    @JvmField
    val loadingIcon = IconLoader.getIcon("/icons/loading.gif", EmbraceIcons::class.java)
}