package com.hmydk.aigit;

import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.vcs.commit.CommitWorkflowUi;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommitSelectionTest {

    @Test
    void returnsNullWhenCommitUiIsUnavailable() {
        assertNull(CommitSelection.from(null, commitMessage(new AtomicReference<>())));
        assertNull(CommitSelection.from(workflowUi(List.of()), null));
    }

    @Test
    void snapshotsIncludedFilesThroughPublicInterfaces() {
        List<FilePath> files = new ArrayList<>();
        files.add(filePath("/repo/new.txt"));
        AtomicReference<String> message = new AtomicReference<>();
        CommitMessageI commitMessage = commitMessage(message);

        CommitSelection selection = CommitSelection.from(workflowUi(files), commitMessage);
        files.clear();

        assertFalse(selection.isEmpty());
        assertEquals(1, selection.includedUnversionedFiles().size());
        assertEquals("/repo/new.txt", selection.includedUnversionedFiles().get(0).getPath());
        assertSame(commitMessage, selection.commitMessage());

        selection.commitMessage().setCommitMessage("feat: selected change");
        assertEquals("feat: selected change", message.get());
    }

    @Test
    void reportsEmptySelection() {
        CommitSelection selection = CommitSelection.from(
                workflowUi(List.of()),
                commitMessage(new AtomicReference<>())
        );

        assertTrue(selection.isEmpty());
    }

    private CommitWorkflowUi workflowUi(List<FilePath> unversionedFiles) {
        return proxy(CommitWorkflowUi.class, (method, args) -> switch (method.getName()) {
            case "getIncludedChanges", "getDisplayedChanges" -> List.of();
            case "getIncludedUnversionedFiles", "getDisplayedUnversionedFiles" -> unversionedFiles;
            default -> defaultValue(method.getReturnType());
        });
    }

    private CommitMessageI commitMessage(AtomicReference<String> message) {
        return proxy(CommitMessageI.class, (method, args) -> {
            if ("setCommitMessage".equals(method.getName()) && args != null) {
                message.set((String) args[0]);
            }
            return null;
        });
    }

    private FilePath filePath(String path) {
        return proxy(FilePath.class, (method, args) -> switch (method.getName()) {
            case "getPath", "getPresentableUrl" -> path;
            case "getName" -> "new.txt";
            default -> defaultValue(method.getReturnType());
        });
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> type.getSimpleName() + " test proxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    }
                    return invocation.invoke(method, args);
                }
        );
    }

    private Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return 0;
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(Method method, Object[] args);
    }
}
