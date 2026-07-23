package com.hmydk.aigit.context;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileChangeSelectedHunkTest {

    @Test
    void buildsPromptDiffFromPreparedSelectedRevisionOnly() {
        FilePath path = filePath("/repo/src/Example.java");
        ContentRevision before = revision(path, "old selected\nstable line");
        ContentRevision selectedAfter = revision(path, "new selected\nstable line");
        Change selectedChange = new Change(before, selectedAfter);

        List<FileChange> result = FileChange.fromGitChanges(List.of(selectedChange), List.of());

        assertEquals(1, result.size());
        String diff = result.get(0).getDiffContent();
        assertTrue(diff.contains("-old selected"));
        assertTrue(diff.contains("+new selected"));
        assertFalse(diff.contains("unchecked working-tree change"));
    }

    private ContentRevision revision(FilePath path, String content) {
        return proxy(ContentRevision.class, (method, args) -> switch (method.getName()) {
            case "getFile" -> path;
            case "getContent" -> content;
            default -> defaultValue(method.getReturnType());
        });
    }

    private FilePath filePath(String path) {
        return proxy(FilePath.class, (method, args) -> switch (method.getName()) {
            case "getPath", "getPresentableUrl" -> path;
            case "getName" -> "Example.java";
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
