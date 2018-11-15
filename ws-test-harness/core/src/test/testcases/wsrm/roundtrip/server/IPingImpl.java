/*
 * Copyright (c) 2006, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package wsrm.roundtrip.server;
import javax.xml.bind.JAXBElement;
import javax.jws.WebService;

import javax.xml.bind.*;
import javax.xml.namespace.*;

import javax.jws.WebService;
import javax.jws.WebParam;
import javax.xml.bind.JAXBElement;
import javax.xml.ws.soap.*;
import javax.xml.ws.*;

@WebService(endpointInterface="wsrm.roundtrip.server.IPing", targetNamespace="http://tempuri.org/", serviceName="PingService", portName="WSHttpBinding_IPing")
@BindingType(SOAPBinding.SOAP12HTTP_BINDING)
public class IPingImpl {

    /**
     * @param String
     */
       public PingResponseBodyType echoString(
        PingRequestBodyType echoString){
        PingResponseBodyType pr = new ObjectFactory().createPingResponseBodyType();
        JAXBElement<String> val = new JAXBElement<String>(new QName("http://tempuri.org/","EchoStringReturn"),String.class,new String("Returning hello "));
        JAXBElement<String> text = echoString.getText();
        JAXBElement<String> seq = echoString.getSequence();
        val.setValue("Returning " +text.getValue()+"Sequence" +seq.getValue());
        pr.setEchoStringReturn(val);

        return pr;
    }
}
