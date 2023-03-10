package io.embrace.android.intellij.plugin.forms;

import com.intellij.openapi.wm.ToolWindow;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;

public class MainForm {
    private JPanel panel;


    public MainForm(ToolWindow toolWindow) {

        panel = new JPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = createLabel("Get started with Embrace", TEXT_LVL.HEADLINE_1);
        panel.add(title);

        JLabel description = createLabel("Add Embrace to your app to help you track, prioritize, and fix stability issues that erode your app quality.", TEXT_LVL.BODY);
        description.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        panel.add(description);

        JLabel step1 = createLabel("1. Connect your app to Embrace", TEXT_LVL.HEADLINE_2);
        step1.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        panel.add(step1);

        JButton connectToEmbraceButton = new JButton("Connect to Embrace");
        connectToEmbraceButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        connectToEmbraceButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        connectToEmbraceButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        connectToEmbraceButton.addActionListener(e -> {
            // define the URL to open in the browser
            String url = "https://dash.embrace.io/onboard/project";
            // open the URL in the default browser
            try {
                Desktop.getDesktop().browse(new java.net.URI(url));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        panel.add(connectToEmbraceButton);


        JLabel step2 = createLabel("2. Add Embrace SDK and Swazzler plugin to your app", TEXT_LVL.HEADLINE_2);
        step2.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        panel.add(step2);

    }

    private enum TEXT_LVL {
        HEADLINE_1,
        HEADLINE_2,
        HEADLINE_3,
        BODY,
    }

    private JLabel createLabel(String text, TEXT_LVL lvl) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        switch (lvl) {

            case HEADLINE_1:
                label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
                break;
            case HEADLINE_2:
                label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
                break;
            case HEADLINE_3:
                label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                break;
            case BODY:
                label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                break;
        }


        return label;
    }

    public JPanel getContent() {
        return panel;
    }

}
