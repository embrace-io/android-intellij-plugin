package io.embrace.android.intellij.plugin.forms;

import com.intellij.openapi.wm.ToolWindow;

import javax.swing.*;

public class MainForm {
    private JPanel container;
    private JButton openDashboardButton;

    public MainForm(ToolWindow toolWindow) {

    }

    public JPanel getContent() {
        return container;
    }

}
