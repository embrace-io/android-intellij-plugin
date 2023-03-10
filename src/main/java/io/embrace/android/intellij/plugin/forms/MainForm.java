package io.embrace.android.intellij.plugin.forms;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.SystemIndependent;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public class MainForm {
    private final JPanel panel;
    private final JScrollPane scrollPane;


    public MainForm(ToolWindow toolWindow, @NotNull Project project) {
        panel = new JPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = createLabel("Get started with Embrace", TEXT_LVL.HEADLINE_1);
        panel.add(title);

        JLabel description = createLabel("Add Embrace to your app to help you track, prioritize, and fix stability ", TEXT_LVL.BODY);
        description.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        panel.add(description);

        description = createLabel("issues that erode your app quality.", TEXT_LVL.BODY);
        description.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        panel.add(description);

        JLabel label = createLabel("1. Connect your app to Embrace", TEXT_LVL.HEADLINE_2);
        label.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        panel.add(label);

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

        label = createLabel("2. Add the embrace-config file", TEXT_LVL.HEADLINE_2);
        label.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        panel.add(label);

        JButton createFileButton = new JButton("Create configuration file");
        createFileButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        createFileButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        createFileButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        createFileButton.addActionListener(e -> {
            createEmbraceFile(project.getBasePath());
        });
        panel.add(createFileButton);

        label = createLabel("3. Add Embrace SDK and Swazzler plugin to your app", TEXT_LVL.HEADLINE_2);
        label.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        panel.add(label);

        label = createLabel("In your project-level build.gradle file, add:", TEXT_LVL.BODY);
        label.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        panel.add(label);

        label = getSdkCodeBlock();
        label.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(label);

        label = createLabel("Add the Swazzler plugin and the dependencies for the Embrace SDK to your module's app-level Gradle file,", TEXT_LVL.BODY);
        label.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        panel.add(label);

        label = createLabel("normally `app/build.gradle`. Make sure that compileOptions for Java 8 are also added to the file.", TEXT_LVL.BODY);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        panel.add(label);

        label = getSwazzlerCodeBlock();
        label.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(label);

        label = createLabel("", TEXT_LVL.BODY); // empty vertical space
        label.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        panel.add(label);

        JButton addSDKButton = new JButton("Change gradle files");
        addSDKButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        addSDKButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        addSDKButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        addSDKButton.addActionListener(e -> {
            modifyGradleFile(project.getBasePath());
        });
        panel.add(addSDKButton);


        label = createLabel("4. Start Embrace", TEXT_LVL.HEADLINE_2);
        label.setBorder(BorderFactory.createEmptyBorder(20, 0, 16, 0));
        panel.add(label);

        label = createLabel("Start the Embrace SDK object at the top of your Application class:", TEXT_LVL.BODY);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        panel.add(label);

        label = getStartCodeBlock();
        label.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(label);


        // Add the panel to a JScrollPane
        scrollPane = new JScrollPane(panel);
    }

    private void modifyGradleFile(@Nullable String basePath) {
        try {
            File file = new File(basePath + "/build.gradle");


            String sb = "// Top-level build file where you can add configuration options common to all sub-projects/modules.\n" +
                    "buildscript {\n" +
                    "    repositories {\n" +
                    "        google()\n" +
                    "        mavenCentral()\n" +
                    "    }\n" +
                    "    dependencies {\n" +
                    "        classpath \"com.android.tools.build:gradle:7.0.0\"\n" +
                    "        classpath \"org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.10\"\n" +
                    "        classpath \"io.embrace:embrace-swazzler:5.14.0\"\n" +
                    "\n" +
                    "        // NOTE: Do not place your application dependencies here; they belong\n" +
                    "        // in the individual module build.gradle files\n" +
                    "    }\n" +
                    "}\n" +
                    "\n" +
                    "task clean(type: Delete) {\n" +
                    "    delete rootProject.buildDir\n" +
                    "}";

            PrintWriter writer = new PrintWriter(file);
            writer.write(sb);
            writer.close();

        } catch (IOException e) {
            System.out.println("An error occurred reading build.gradle file.");
            e.printStackTrace();
        }
    }

    private void createEmbraceFile(@Nullable String basePath) {
        try {
            File file = new File(basePath + "/app/src/main/embrace-config.json");
            FileWriter writer = new FileWriter(file);
            writer.write("{\n" +
                    "  \"app_id\": \"hU4P8\",\n" +
                    "  \"api_token\": \"13f327e891ad45858949004eb755b9f1\",\n" +
                    "  \"ndk_enabled\": false\n" +
                    "}");
            writer.close();
            System.out.println("File created: " + file.getName());
        } catch (Exception e) {
            System.out.println("An error occurred on embrace-config file creation.");
            e.printStackTrace();
        }

    }

    private JLabel getSdkCodeBlock() {
        // Create a monospaced font for the label
        Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);

        // Create a JLabel with some code text
        JLabel codeLabel = new JLabel();
        codeLabel.setFont(font);
        codeLabel.setText("<html><pre><code>" +
                "buildscript {\n" +
                "  repositories {\n" +
                "    mavenCentral()\n" +
                "    google()\n" +
                "  }\n" +
                "\n" +
                "  dependencies {\n" +
                "    classpath 'io.embrace:embrace-swazzler:5.14.2'\n" +
                "  }\n" +
                "}" +
                "</code></pre></html>");
        codeLabel.setOpaque(true);

        Color backgroundColor = Color.decode("#5c5c5c"); // dark gray
        codeLabel.setBackground(backgroundColor);

        return codeLabel;
    }

    private JLabel getSwazzlerCodeBlock() {
        // Create a monospaced font for the label
        Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);

        // Create a JLabel with some code text
        JLabel codeLabel = new JLabel();
        codeLabel.setFont(font);
        codeLabel.setText("<html><pre><code>" +
                "plugins {\n" +
                "    id 'com.android.application'\n" +
                "    id 'embrace-swazzler'\n" +
                "    ...\n" +
                "}" +
                "</code></pre></html>");
        codeLabel.setOpaque(true);


        Color backgroundColor = Color.decode("#5c5c5c"); // dark gray
        codeLabel.setBackground(backgroundColor);

        return codeLabel;
    }


    private JLabel getStartCodeBlock() {
        // Create a monospaced font for the label
        Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);

        // Create a JLabel with some code text
        JLabel codeLabel = new JLabel();
        codeLabel.setFont(font);
        codeLabel.setText("<html><pre><code>" +
                "import io.embrace.android.embracesdk.Embrace\n" +
                "\n" +
                "class MyApplication : Application() {\n" +
                "    override fun onCreate() {\n" +
                "        super.onCreate()\n" +
                "        Embrace.getInstance().start(this)\n" +
                "        EmbraceSamples.verifyIntegration() // temporarily add this to verify the integration\n" +
                "    }\n" +
                "}" +
                "</code></pre></html>");
        codeLabel.setOpaque(true);


        Color backgroundColor = Color.decode("#5c5c5c"); // dark gray
        codeLabel.setBackground(backgroundColor);

        return codeLabel;
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

    public JScrollPane getContent() {
        return scrollPane;
    }

}
