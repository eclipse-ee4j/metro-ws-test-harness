/*
 * Copyright (c) 1997, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.container.javase;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.client.InterpreterEx;
import com.sun.xml.ws.test.container.AbstractHttpApplication;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.container.DeployedService;

import java.net.URL;

/**
 * {@link Application} implementation for {@link JavaSeContainer}.
 *
 * @author Jitendra Kotamraju
 */
final class JavaSeApplication extends AbstractHttpApplication {


    /**
     * <tt>jakarta.xml.ws.Endpoint</tt> objects. This is loaded in another classloader,
     * so we can't use a typed value.
     */
    private final @NotNull Object[] servers;


    JavaSeApplication(Object[] servers, URL baseAddress, DeployedService service) {
        super(baseAddress, service);
        this.servers = servers;
    }

    /**
     * Removes this application from the container.
     */
    public void undeploy() throws Exception {
        for(Object server : servers) {
            InterpreterEx i = new InterpreterEx(server.getClass().getClassLoader());
            i.set("server",server);
            i.eval("server.stop()");
        }
    }
}

