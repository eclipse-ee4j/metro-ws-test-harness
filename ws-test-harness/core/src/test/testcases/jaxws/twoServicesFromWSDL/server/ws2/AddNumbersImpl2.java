/*
 * Copyright (c) 2012, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package jaxws.twoServicesFromWSDL.server.ws2;

import javax.jws.WebService;
import org.example.AddNumbersFault_Exception;

/**
 *
 * @author ljungman
 */
@WebService(serviceName = "AddNumbersService2", portName = "AddNumbersPort", endpointInterface = "org.example.AddNumbersPortType", targetNamespace = "http://example.org", wsdlLocation = "/WEB-INF/wsdl/ws2/AddNumbers.wsdl")
public class AddNumbersImpl2 {

    public int addNumbers(int arg0, int arg1) throws AddNumbersFault_Exception {
        //TODO implement this method
        return 2242;
    }

    public void oneWayInt(int arg0) {
        //TODO implement this method
        throw new UnsupportedOperationException("Not implemented yet.");
    }

}
