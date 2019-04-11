/*
 * Copyright (c) 1997, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.client;

import javax.xml.transform.stream.StreamSource;

/**
 * XML Resource to be injected to the client.
 * Defiend by &lt;xml-resource&gt; element in test descriptor.
 *
 * @author Kohsuke Kawaguchi
 */
public interface XmlResource {
    /**
     * Returns this XML as a {@link StreamSource}.
     */
    StreamSource asStreamSource() throws Exception;
    /**
     * Returns this XML as a String literal.
     */
    String asString() throws Exception;
    /**
     * Reads it as SAAJ SOAPMessage in SOAP 1.1 and return it.
     */
    Object asSOAP11Message() throws Exception;
}
