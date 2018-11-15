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

import org.dom4j.Element;
import org.dom4j.io.XMLWriter;

import java.io.StringWriter;

/**
 * Resource XML defined inline.
 *
 * @author Kohsuke Kawaguchi
 */
public class InlineXmlResource extends AbstractXmlResource {
    private final Element root;

    public InlineXmlResource(Element root) {
        this.root = root;
    }

    public String asString() throws Exception {
        StringWriter sw = new StringWriter();
        new XMLWriter(sw).write(root);
        return sw.toString();
    }
}
