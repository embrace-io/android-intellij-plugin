package io.embrace.android.intellij.plugin.forms;

import com.intellij.openapi.wm.ToolWindow;

import javax.swing.*;
import java.awt.*;

public class MainForm {
    private JPanel container;
    private JButton connectToEmbraceButton;

    public MainForm(ToolWindow toolWindow) {


        connectToEmbraceButton.addActionListener(e -> {
            // define the URL to open in the browser
            String url = "https://dash.embrace.io/";

            // open the URL in the default browser
            try {
                Desktop.getDesktop().browse(new java.net.URI(url));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    public JPanel getContent() {
        return container;
    }

}
