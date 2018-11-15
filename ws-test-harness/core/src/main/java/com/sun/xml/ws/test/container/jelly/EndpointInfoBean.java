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

import javax.xml.namespace.QName;

/**
 * This bean wraps the endpoint information. It is passed to
 * the Jelly script to create a sun-jaxws.xml file.
 */
public class EndpointInfoBean {

    private final String name;
    private final String implementation;
    private final String wsdl;
    private final QName service;
    private final QName port;
    private final String binding;
    private final String urlPattern;

    // 'binding' can be null
    EndpointInfoBean(String name, String impl, String wsdl,
        QName service, QName port, String binding, String urlPattern) {
        
        this.name = name;
        this.implementation = impl;
        this.wsdl = wsdl;
        this.service = service;
        this.port = port;
        this.binding = binding;
        this.urlPattern = urlPattern;
    }

    /**
     * Just so that we can invoke this method from beanshell.
     */
    public static EndpointInfoBean create(String name, String impl, String wsdl,
        QName service, QName port, String binding, String urlPattern) {

        return new EndpointInfoBean(name,impl,wsdl,service,port,binding,urlPattern);
    }


    public String getName() {
        return name;
    }

    public String getImplementation() {
        return implementation;
    }
    
    public String getWsdl() {
        return wsdl;
    }
    
    public String getService() {
        return (service == null ? null : service.toString());
    }

    public QName getServiceName() {
        return service;
    }
    
    public String getPort() {
        return (port == null ? null : port.toString());
    }

    public QName getPortName() {
        return port;
    }
    
    public String getBinding() {
        return binding;
    }
    
    public String getUrlPattern() {
        return urlPattern;
    }
    
}
