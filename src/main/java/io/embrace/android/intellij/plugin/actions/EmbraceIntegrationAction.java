package io.embrace.android.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class EmbraceIntegrationAction extends AnAction {


    public EmbraceIntegrationAction() {
    }

    /**
     * This constructor is used to support dynamically added menu actions.
     * It sets the text, description to be displayed for the menu item.
     * Otherwise, the default AnAction constructor is used by the IntelliJ Platform.
     *
     * @param text        The text to be displayed as a menu item.
     * @param description The description of the menu item.
     * @param icon        The icon to be used with the menu item.
     */
    public EmbraceIntegrationAction(String text, String description, Icon icon) {
        super(text, description, icon);
    }


    /**
     * Gives the user feedback when the dynamic action menu is chosen.
     * Pops a simple message dialog. See the psi_demo plugin for an
     * example of how to use [AnActionEvent] to access data.
     *
     * @param event Event received when the associated menu item is chosen.
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(event.getProject());
        ToolWindow window = toolWindowManager.getToolWindow("Embrace Integration Assistant");
        if (window != null)
            window.show();
    }


//    /**
//     * Determines whether this menu item is available for the current context.
//     * Requires a project to be open.
//     *
//     * @param e Event received when the associated group-id menu is chosen.
//     */
//    @Override
//    public void update(@NotNull AnActionEvent e) {
//        super.update(e); //        e.presentation.isEnabledAndVisible = e.project != null
//
//    }

}
