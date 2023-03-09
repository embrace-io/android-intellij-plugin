// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package io.embrace.android.intellij.plugin.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import io.embrace.android.intellij.plugin.icons.EmbraceIcons.embrace_default_icon

/**
 * Creates an action group to contain menu actions. See plugin.xml declarations.
 */
class CustomDefaultActionGroup : DefaultActionGroup() {
    /**
     * Given [CustomDefaultActionGroup] is derived from [com.intellij.openapi.actionSystem.ActionGroup],
     * in this context `update()` determines whether the action group itself should be enabled or disabled.
     * Requires an editor to be active in order to enable the group functionality.
     *
     * @param event Event received when the associated group-id menu is chosen.
     * @see com.intellij.openapi.actionSystem.AnAction.update
     */
    override fun update(event: AnActionEvent) {
        // Enable/disable depending on whether user is editing
        val editor = event.getData(CommonDataKeys.EDITOR)
        event.presentation.isEnabled = editor != null
        // Take this opportunity to set an icon for the group.
        event.presentation.setIcon(embrace_default_icon)
    }
}