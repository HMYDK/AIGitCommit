package com.hmydk.aigit.constant;

/**
 * Constants
 *
 * @author hmydk
 */
public class Constants {

    public static final String NO_FILE_SELECTED = "No file selected";
    public static final String GENERATING_COMMIT_MESSAGE = "Generating...";
    public static final String TASK_TITLE = "Generating commit message";

    public static final String[] languages = {"English", "Chinese", "Japanese", "Korean", "French", "Spanish",
            "German", "Russian", "Arabic", "Portuguese"};



    public static final String PROJECT_PROMPT_FILE_NAME = "commit-prompt.txt";
    public static final String PROJECT_PROMPT  = "Project Prompt";
    public static final String CUSTOM_PROMPT  = "Custom Prompt";

    public static String[] getAllPromptTypes() {
        return new String[]{PROJECT_PROMPT, CUSTOM_PROMPT};
    }

}
