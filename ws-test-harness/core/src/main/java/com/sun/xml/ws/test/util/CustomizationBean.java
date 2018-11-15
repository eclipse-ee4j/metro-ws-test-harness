/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

/*
 * CustomizationBean.java
 *
 * Created on July 14, 2006, 9:59 AM
 */

package com.sun.xml.ws.test.util;

/**
 * Bean used to pass parameters to custom-client.xml/custom-server.xml
 * generation.
 *
 * @author WS Test Harness Team
 */
public class CustomizationBean {
    
    private String packageName;
    private String wsdlFileName;

    public CustomizationBean(String packageName, String wsdlFileName) {
        this.packageName = packageName;
        this.wsdlFileName = wsdlFileName;
    }
    
    public String getPackageName() {
        return packageName;
    }
    
    public String getWsdlFileName() {
        return wsdlFileName;
    }
}
