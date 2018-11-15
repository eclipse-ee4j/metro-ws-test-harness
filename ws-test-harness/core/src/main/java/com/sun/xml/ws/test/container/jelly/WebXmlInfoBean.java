/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.container.jelly;

import com.sun.xml.ws.test.container.DeploymentContext;

import java.util.List;

/**
 * This bean wraps the endpoint information. It is passed to
 * the Jelly script to create a web.xml file.
 * <p>
 * The field names match the element names in the web.xml
 * template.
 */
public class WebXmlInfoBean {

    private final String description;
    private final String displayName;
    private final String servletName;
    private final List<EndpointInfoBean> endpoints;
    private final String listenerClass;
    private final String servletClass;

    /**
     * The constructor creates the fields queried by the Jelly script.
     * In many jax-ws web.xml files, the servlet name and display
     * name are the same. This same convention is followed here
     * for simplicity.
     * <p>
     * TODO: support for multiple ports. Currently hard-coding
     * url pattern and assuming only one port/service.
     */
    public WebXmlInfoBean(DeploymentContext context, List<EndpointInfoBean> endpoints,
                          String listenerClass, String servletClass) {
        description = context.descriptor.description;
        displayName = context.descriptor.name;
        servletName = context.descriptor.name;
        this.listenerClass = listenerClass;
        this.servletClass = servletClass;
        this.endpoints = endpoints;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getServletName() {
        return servletName;
    }

    public String getServletClass() {
        return servletClass;
    }

    public String getListenerClass() {
        return listenerClass;
    }

    /**
     * Starting from wsdl, a service may have more than
     * one port. So the web.xml will have more than one
     * url mapping to the same jax-ws servlet. The
     * mappings in web.xml should match the endpoints
     * in sun-jaxws.xml.
     */
    public List<EndpointInfoBean> getEndpoints() {
        return endpoints;
    }

}
