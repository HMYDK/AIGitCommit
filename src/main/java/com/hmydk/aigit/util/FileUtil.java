package com.hmydk.aigit.util;

import com.hmydk.aigit.constant.Constants;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class FileUtil {

    public static String readFile(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        reader.close();
        return content.toString();
    }

    public static String loadProjectPrompt() {
        String res = "";
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length > 0) {
            Project currentProject = openProjects[0];
            File promptFile = new File(currentProject.getBasePath(), Constants.PROJECT_PROMPT_FILE_NAME);
            if (promptFile.exists()) {
                try {
//                    String content = new String(java.nio.file.Files.readAllBytes(promptFile.toPath()));
                    res = FileUtil.readFile(promptFile.getPath());
                } catch (Exception ex) {
                    res = "Error reading project prompt file: " + ex.getMessage();
                    throw new IllegalArgumentException(res);
                }
            } else {
                res = "No " + Constants.PROJECT_PROMPT_FILE_NAME + " file found in the project root directory : " + currentProject.getBasePath();
                throw new IllegalArgumentException(res);
            }
        } else {
            res = "No open project found.";
            throw new IllegalArgumentException(res);
        }

        if (res.isEmpty()) {
            throw new IllegalArgumentException("No content found in the project prompt file.");
        }

        return res;
    }
}
