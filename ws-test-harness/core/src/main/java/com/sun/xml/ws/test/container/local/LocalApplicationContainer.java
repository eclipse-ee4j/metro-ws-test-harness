/*
 * Copyright (c) 1997, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.container.local;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.container.AbstractApplicationContainer;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.container.WAR;
import com.sun.xml.ws.test.tool.WsTool;
import com.sun.xml.ws.test.util.XMLUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.List;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * {@link ApplicationContainer} for the local transport.
 *
 * @deprecated
 *      To be removed once in-vm transport becomes ready
 * @author Kohsuke Kawaguchi
 */
@Deprecated
public class LocalApplicationContainer extends AbstractApplicationContainer {

    public LocalApplicationContainer(WsTool wsimport, WsTool wsgen) {
        super(wsimport,wsgen,false);
    }

    public String getTransport() {
        return "local";
    }

    public void start() {
        // noop
    }

    public void shutdown() {
        // noop
    }

    @NotNull
    public Application deploy(DeployedService service) throws Exception {
        WAR war = assembleWar(service);
        if (service.service.isSTS) {
            String newLocation = "local://" + service.warDir.getAbsolutePath() + "/";
            newLocation = newLocation.replace('\\', '/');
            updateWsitClient(war, service, newLocation);
        }
        for (File wsdl : war.getWSDL())
            patchWsdl(service,wsdl);
        return new LocalApplication(war,new URI("local://" +
            service.warDir.getAbsolutePath().replace('\\','/')));
    }

    /**
     * Fix the address in the WSDL. to the local address.
     */
    private void patchWsdl(DeployedService service, File wsdl) throws Exception {
        Document doc = XMLUtil.readXML(wsdl, null);
        List<Element> ports = XMLUtil.getElements(doc, "//*[local-name()='service']/*");

        for (Element port : ports) {
            String portName = port.getAttribute("name");

            Element address = (Element)port.getFirstChild();

            Attr locationAttr = address.getAttributeNode("location");
            String newLocation =
                "local://" + service.warDir.getAbsolutePath() + "?" + portName;
            newLocation = newLocation.replace('\\', '/');
            locationAttr.setValue(newLocation);

            //Patch wsa:Address in wsa:EndpointReference as well
            NodeList nl = port.getElementsByTagNameNS("http://www.w3.org/2005/08/addressing", "EndpointReference");
            Element wsaEprEl = nl.getLength() > 0 ? (Element) nl.item(0) : null;
            if (wsaEprEl != null) {
                nl = wsaEprEl.getElementsByTagNameNS("http://www.w3.org/2005/08/addressing", "Address");
                Element wsaAddrEl = nl.getLength() > 0 ? (Element) nl.item(0) : null;
                if (wsaAddrEl != null) {
                    wsaAddrEl.setTextContent(newLocation);
                }

            }
        }

        // save file
        FileOutputStream os = new FileOutputStream(wsdl);
        XMLUtil.writeXML(doc, os);
        os.close();
    }

}
