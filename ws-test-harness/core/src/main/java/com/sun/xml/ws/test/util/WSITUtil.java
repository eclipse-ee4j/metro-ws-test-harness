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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author lukas
 */
public final class WSITUtil {

    private WSITUtil() {
    }

    public static void updateWsitClient(File wsitClientFile, String endpointUri, String wsdlLocation) throws Exception {
        Document document = XMLUtil.readXML(wsitClientFile, null);
        Element root = document.getDocumentElement();
        Element sts = XMLUtil.getElements(root, "//*[local-name()='Policy']/*[local-name()='ExactlyOne']/*[local-name()='All']/*[local-name()='PreconfiguredSTS']").get(0);

        Attr endpoint = sts.getAttributeNode("endpoint");
        endpoint.setValue(endpointUri);

        Attr wsdlLoc = sts.getAttributeNode("wsdlLocation");
        wsdlLoc.setValue(wsdlLocation);

        for (Element keystore : XMLUtil.getElements(root, "//*[local-name()='KeyStore']")) {
            Attr loc = keystore.getAttributeNode("location");
            loc.setValue(loc.getValue().replaceAll("\\$WSIT_HOME", System.getProperty("WSIT_HOME")));
        }

        for (Element truststore : XMLUtil.getElements(root, "//*[local-name()='TrustStore']")) {
            Attr loc = truststore.getAttributeNode("location");
            loc.setValue(loc.getValue().replaceAll("\\$WSIT_HOME", System.getProperty("WSIT_HOME")));
        }

        try (OutputStream os = new FileOutputStream(wsitClientFile)) {
            XMLUtil.writeXML(document, os);
            os.flush();
        }
    }
}
