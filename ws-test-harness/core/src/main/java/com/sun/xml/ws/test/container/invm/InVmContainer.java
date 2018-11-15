/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
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
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URLClassLoader;
import java.net.URL;
import java.util.List;

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
        Document doc = new SAXReader().read(wsdl);
        List<Element> ports = doc.getRootElement().element("service").elements("port");

        for (Element port : ports) {
            String portName = port.attributeValue("name");
            Element address = getSoapAddress(port);

            //Looks like invalid wsdl:port, MUST have a soap:address
            //TODO: give some error message
            if (address == null)
                continue;

            if (!"wsdl".equalsIgnoreCase(wsdl.getParentFile().getName())) {
                portName = wsdl.getParentFile().getName() + portName;
            }

            Attribute locationAttr = address.attribute("location");
            String newLocation =
                    "in-vm://" + id + "/?" + portName;
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

    private Element getSoapAddress(Element port){
        for(Object obj : port.elements()){
            Element address = (Element) obj;

            //it might be extensibility element, just skip it
            if(!address.getName().equals("address"))
                continue;

            if(address.getNamespaceURI().equals("http://schemas.xmlsoap.org/wsdl/soap/") ||
                   address.getNamespaceURI().equals("http://schemas.xmlsoap.org/wsdl/soap12/"))
            return address;
        }
        return null;
    }

}
