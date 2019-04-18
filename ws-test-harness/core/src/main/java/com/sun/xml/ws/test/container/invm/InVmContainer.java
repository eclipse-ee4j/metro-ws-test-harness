/*
 * Copyright (c) 1997, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.container.invm;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.container.AbstractApplicationContainer;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.container.WAR;
import com.sun.xml.ws.test.tool.WsTool;
import com.sun.xml.ws.test.World;
import com.sun.xml.ws.test.client.InterpreterEx;
import com.sun.xml.ws.test.util.XMLUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLClassLoader;
import java.net.URL;
import java.util.List;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * {@link ApplicationContainer} for the local transport.
 *
 * @author Kohsuke Kawaguchi
 */
public class InVmContainer extends AbstractApplicationContainer {

    public InVmContainer(WsTool wsimport, WsTool wsgen) {
        super(wsimport,wsgen,false);
    }

    public String getTransport() {
        return "in-vm";
    }

    public void start() {
        // noop
    }

    public void shutdown() {
        // noop
    }

    @NotNull
    public Application deploy(DeployedService service) throws Exception {
        String id = service.service.getGlobalUniqueName();
        WAR war = assembleWar(service);

        if (service.service.isSTS) {
            String newLocation = "in-vm://" + id + "/";
            newLocation = newLocation.replace('\\', '/');
            updateWsitClient(war, service, newLocation);
        }

        for (File wsdl : war.getWSDL())
            patchWsdl(service,wsdl,id);

        URLClassLoader serviceClassLoader = new URLClassLoader(
            new URL[]{new File(service.warDir,"WEB-INF/classes").toURL()},
            World.runtime.getClassLoader());
        InterpreterEx i = new InterpreterEx(serviceClassLoader);
        i.set("id",id);
        i.set("dir",service.warDir);
        Object server = i.eval("new com.sun.xml.ws.transport.local.InVmServer(id,dir)");

        return new InVmApplication(war,server,new URI("in-vm://"+id+"/"));
    }

    /**
     * Fix the address in the WSDL. to the local address.
     */
    private void patchWsdl(DeployedService service, File wsdl, String id) throws Exception {
        Document doc = XMLUtil.readXML(wsdl, null);

        if (service.service.isSTS) {
            for (Element keystore : XMLUtil.getElements(doc, "//*[local-name()='KeyStore']")) {
                Attr loc = keystore.getAttributeNode("location");
                loc.setValue(loc.getValue().replaceAll("\\$WSIT_HOME", System.getProperty("WSIT_HOME")));
            }
            for (Element truststore : XMLUtil.getElements(doc, "//*[local-name()='TrustStore']")) {
                Attr loc = truststore.getAttributeNode("location");
                loc.setValue(loc.getValue().replaceAll("\\$WSIT_HOME", System.getProperty("WSIT_HOME")));
            }
        }

        List<Element> ports = XMLUtil.getElements(doc, "//*[local-name()='service']/*");

        for (Element port : ports) {
            String portName = port.getAttribute("name");
            Element address = getSoapAddress(port);

            //Looks like invalid wsdl:port, MUST have a soap:address
            //TODO: give some error message
            if (address == null)
                continue;

            if (!"wsdl".equalsIgnoreCase(wsdl.getParentFile().getName())) {
                portName = wsdl.getParentFile().getName() + portName;
            }

            Attr locationAttr = address.getAttributeNode("location");
            String newLocation =
                    "in-vm://" + id + "/?" + portName;
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
        try (OutputStream os = new FileOutputStream(wsdl)) {
            XMLUtil.writeXML(doc, os);
            os.flush();
        }
    }

    private Element getSoapAddress(Element port){
        for(Element address : XMLUtil.getChildren(port, Element.class)){
            //it might be extensibility element, just skip it
            if (!"address".equals(address.getLocalName())) {
                continue;
            }
            if("http://schemas.xmlsoap.org/wsdl/soap/".equals(address.getNamespaceURI()) ||
                   "http://schemas.xmlsoap.org/wsdl/soap12/".equals(address.getNamespaceURI()))
            return address;
        }
        return null;
    }

}
