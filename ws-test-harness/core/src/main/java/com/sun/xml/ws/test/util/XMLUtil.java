/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.sun.xml.ws.test.util;

import com.thaiopensource.relaxng.jarv.RelaxNgCompactSyntaxVerifierFactory;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.iso_relax.jaxp.ValidatingDocumentBuilderFactory;
import org.iso_relax.verifier.Schema;
import org.iso_relax.verifier.VerifierConfigurationException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 *
 * @author lukas
 */
public final class XMLUtil {

    private static Schema s;
    private static String url;

    private XMLUtil() {
        // private
    }

    public static Document readXML(File f, URL schemaUrl) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory;
        if (schemaUrl != null) {
            String ef = schemaUrl.toExternalForm();
            if (!ef.equalsIgnoreCase(url)) {
                try {
                    s = new RelaxNgCompactSyntaxVerifierFactory().compileSchema(ef);
                } catch (SAXParseException e) {
                    throw new Error("unable to parse test-descriptor.rnc at line " + e.getLineNumber(), e);
                } catch (IOException | VerifierConfigurationException | SAXException e) {
                    throw new Error("unable to parse test-descriptor.rnc", e);
                }
                url = ef;
            }
            factory = new VP(s);
        } else {
            factory = DocumentBuilderFactory.newInstance();
        }
        factory.setNamespaceAware(true);
        factory.setValidating(schemaUrl != null);
        return factory.newDocumentBuilder().parse(f);
    }

    public static void writeXML(Document doc, OutputStream os) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(os);
        // Output to console for testing
        // StreamResult result = new StreamResult(System.out);
        transformer.transform(source, result);
    }

    public static List<Element> getElements(Node node, String xpath) throws IOException {
        XPath x = XPathFactory.newInstance().newXPath();
        NodeList nodes;
        try {
            nodes = (NodeList) x.evaluate(xpath, node, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new IOException(e);
        }
        if (nodes == null || nodes.getLength() <= 0) {
            return Collections.EMPTY_LIST;
        }
        List<Element> list = new ArrayList<>(nodes.getLength());
        for (int i = 0; i < nodes.getLength(); i++) {
            list.add((Element) nodes.item(i));

        }
        return list;
    }

    public static <T> List<T> getChildren(Node n, Class<T> cls) {
        NodeList nl = n.getChildNodes();
        if (nl == null || nl.getLength() < 1) {
            return Collections.EMPTY_LIST;
        }
        List<T> list = new ArrayList<>(nl.getLength());
        for (int i = 0; i < nl.getLength(); i++) {
            Object o = nl.item(i);
            if (cls.isAssignableFrom(o.getClass())) {
                list.add((T) nl.item(i));
            }
        }
        return list;
    }

    public static String getTextFrom(Node n) {
        List<Text> tn = getChildren(n, Text.class);
        StringBuilder sb = new StringBuilder();
        for (Text t : tn) {
            sb.append(t.getNodeValue());
        }
        return sb.toString();
    }

    public static String getTextFrom(Node n, String xpath) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Element x : getElements(n, xpath)) {
            sb.append(getTextFrom(x));
        }
        String s = sb.toString().trim();
        return s.isEmpty() ? null : s;
    }

    public static String getAttributeOrNull(Element e, String attrName) {
        return getAttributeOrDefault(e, attrName, null);
    }

    public static String getAttributeOrDefault(Element e, String attrName, String def) {
        String s = "";
        Attr attributeNode = e.getAttributeNode(attrName);
        if (attributeNode != null) {
            s = attributeNode.getValue();
        }
        return s.trim().isEmpty() ? def : s;
    }

    private static class VP extends ValidatingDocumentBuilderFactory {

        private VP(Schema descriptorSchema) {
            super(DocumentBuilderFactory.newInstance(), descriptorSchema);
        }

    }
}
