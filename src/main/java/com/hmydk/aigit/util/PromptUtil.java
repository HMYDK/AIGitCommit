package com.hmydk.aigit.util;
import java.util.List;

/**
 * PromptUtil
 *
 * @author hmydk
 */
public class PromptUtil {

    public static String generatePrompt(List<String> changeContents) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Generate a concise and informative Git commit message based on the following code changes:\n\n");

        for (String change : changeContents) {
            promptBuilder.append(change).append("\n---\n");
        }

        promptBuilder.append("\nThe commit message should:\n");
        promptBuilder.append("1. Summarize the main changes in a single line (50-72 characters)\n");
        promptBuilder.append("2. Provide more detailed explanations in subsequent lines, if necessary\n");
        promptBuilder.append("3. Use the imperative mood (e.g., 'Add feature' not 'Added feature')\n");
        promptBuilder.append("4. Mention any breaking changes\n");
        promptBuilder.append("5. Reference relevant issue numbers if applicable\n");

        return promptBuilder.toString();
    }
}
