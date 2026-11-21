package com.hmydk.aigit;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the Marketplace integration declared in {@code plugin.xml}.
 *
 * <p>Registering {@code JetBrainsMarketplaceErrorReportSubmitter} is what opts the plugin in to
 * exception and UI-freeze reports on JetBrains Marketplace. Without it, users cannot send reports
 * to the plugin author and only freezes that the IDE attributes by heuristics are visible.</p>
 */
class PluginDescriptorTest {

    private static final String MARKETPLACE_ERROR_HANDLER =
            "com.intellij.diagnostic.JetBrainsMarketplaceErrorReportSubmitter";

    @Test
    void registersTheMarketplaceErrorHandler() throws Exception {
        Document pluginXml = readPluginXml();
        NodeList handlers = pluginXml.getElementsByTagName("errorHandler");

        assertEquals(1, handlers.getLength(), "plugin.xml must register exactly one errorHandler");
        Element handler = (Element) handlers.item(0);
        assertEquals(MARKETPLACE_ERROR_HANDLER, handler.getAttribute("implementation"));

        Node parent = handler.getParentNode();
        assertTrue(parent instanceof Element && "extensions".equals(((Element) parent).getTagName()),
                "the errorHandler must be declared inside <extensions>");
        assertEquals("com.intellij", ((Element) parent).getAttribute("defaultExtensionNs"),
                "the errorHandler is a JetBrains Platform extension (com.intellij namespace)");
    }

    @Test
    void marketplaceErrorHandlerClassIsOnThePlatformClasspath() throws Exception {
        Class<?> handler = Class.forName(MARKETPLACE_ERROR_HANDLER, false, getClass().getClassLoader());
        assertNotNull(handler);
    }

    private Document readPluginXml() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/META-INF/plugin.xml")) {
            assertNotNull(input, "META-INF/plugin.xml must be on the test classpath");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            return factory.newDocumentBuilder().parse(input);
        }
    }
}
