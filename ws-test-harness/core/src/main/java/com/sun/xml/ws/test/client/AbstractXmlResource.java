/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.client;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;

/**
 * Partial default implementation.
 * 
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractXmlResource implements XmlResource {
    public StreamSource asStreamSource() throws Exception {
        return new StreamSource(new StringReader(asString()));
    }

    public Object asSOAP11Message() throws Exception {
        InterpreterEx i = new InterpreterEx(Thread.currentThread().getContextClassLoader());
        i.set("res",this);
        return i.eval(
            "factory = MessageFactory.newInstance();\n" +
            "message = factory.createMessage();\n" +
            "message.getSOAPPart().setContent(res.asStreamSource());\n" +
            "message.saveChanges();\n" +
            "return message;");
    }
}
