package com.hmydk.aigit.config;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for adding or editing a prompt
 */
public class PromptDialog extends DialogWrapper {
    private JBTextField promptDescField; // Text field for prompt description
    private JBTextArea promptTextArea;   // Text area for prompt content
    private final String initialDesc;    // Initial description (for edit mode)
    private final String initialText;    // Initial text (for edit mode)
    
    // Constructor for creating a new or editing an existing prompt
    public PromptDialog(@Nullable String initialDesc, @Nullable String initialText) {
        super(true); // use current window as parent
        this.initialDesc = initialDesc;
        this.initialText = initialText;
        setTitle("Prompt Configuration");
        init();
    }
    
    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        // Main panel with vertical layout
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);  // Padding
        
        // Create and add label and input field for "Prompt Description"
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JBLabel("Prompt Description:"), gbc);
        
        promptDescField = new JBTextField(20);
        if (initialDesc != null) {
            promptDescField.setText(initialDesc); // Set initial description if available
        }
        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(promptDescField, gbc);
        
        // Create and add label and input area for "Prompt Text"
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JBLabel("Prompt Text:"), gbc);
        
        promptTextArea = new JBTextArea(5, 20);
        promptTextArea.setLineWrap(true); // Enable line wrapping
        promptTextArea.setWrapStyleWord(true);
        if (initialText != null) {
            promptTextArea.setText(initialText); // Set initial text if available
        }
        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(new JScrollPane(promptTextArea), gbc);
        
        return panel;
    }
    
    // Get the entered prompt description
    public String getPromptDesc() {
        return promptDescField.getText();
    }
    
    // Get the entered prompt text
    public String getPromptText() {
        return promptTextArea.getText();
    }
    
    // Override to validate the inputs (optional, can be expanded based on needs)
    @Override
    protected void doOKAction() {
        if (promptDescField.getText().trim().isEmpty() || promptTextArea.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Both prompt description and text are required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        super.doOKAction(); // Proceed if inputs are valid
    }
}