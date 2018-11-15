/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
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
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.List;

/**
 * {@link ApplicationContainer} for the local transport.
 *
 * @deprecated
 *      To be removed once in-vm transport becomes ready
 * @author Kohsuke Kawaguchi
 */
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
        Document doc = new SAXReader().read(wsdl);
        List<Element> ports = doc.getRootElement().element("service").elements("port");

        for (Element port : ports) {
            String portName = port.attributeValue("name");

            Element address = (Element)port.elements().get(0);

            Attribute locationAttr = address.attribute("location");
            String newLocation =
                "local://" + service.warDir.getAbsolutePath() + "?" + portName;
            newLocation = newLocation.replace('\\', '/');
            locationAttr.setValue(newLocation);

            //Patch wsa:Address in wsa:EndpointReference as well
            Element wsaEprEl = port.element(QName.get("EndpointReference", "wsa", "http://www.w3.org/2005/08/addressing"));
            if (wsaEprEl != null) {
                Element wsaAddrEl = wsaEprEl.element(QName.get("Address", "wsa", "http://www.w3.org/2005/08/addressing"));
                wsaAddrEl.setText(newLocation);

            }
        }

        // save file
        FileOutputStream os = new FileOutputStream(wsdl);
        new XMLWriter(os).write(doc);
        os.close();
    }

}
