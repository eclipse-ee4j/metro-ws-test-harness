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
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.FileInputStream;

/**
 * Resource XML defined as a reference.
 *
 * @author Kohsuke Kawaguchi
 */
public class ReferencedXmlResource extends AbstractXmlResource {
    private final File xml;

    public ReferencedXmlResource(File xml) {
        this.xml = xml;
    }

    public String asString() throws Exception {
        StringWriter sw = new StringWriter();
        Reader r = new InputStreamReader(new FileInputStream(xml),"UTF-8");
        char[] buf = new char[1024];

        int len;
        while((len=r.read(buf))>=0)
            sw.write(buf,0,len);

        r.close();

        return sw.toString();
    }

    public StreamSource asStreamSource() throws Exception {
        return new StreamSource(xml); 
    }
}
