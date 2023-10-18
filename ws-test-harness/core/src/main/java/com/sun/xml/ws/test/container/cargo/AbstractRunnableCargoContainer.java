/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.container.cargo;

import com.sun.xml.ws.test.tool.WsTool;
import java.io.IOException;
import java.net.ServerSocket;
import org.codehaus.cargo.container.LocalContainer;

import java.net.URL;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Common implementation of {@link EmbeddedCargoApplicationContainer}
 * and {@link InstalledCargoApplicationContainer}.
 *
 * This class also assumes that the launched container can be accessible
 * by "localhost".
 *
 * @author Kohsuke Kawaguchi
 */
abstract class AbstractRunnableCargoContainer<C extends LocalContainer> extends AbstractCargoContainer<C> {

    protected final int httpPort;

    protected AbstractRunnableCargoContainer(WsTool wsimport, WsTool wsgen, int port, boolean httpspi) {
        super(wsimport, wsgen, httpspi);
        httpPort = port < 0 ? getFreePort() : port;
    }

    public void start() throws Exception {
        System.out.println("Starting "+container.getId());
        container.start();
    }

    public void shutdown() throws Exception {
        System.out.println("Stopping "+container.getId());
        container.stop();
    }

    protected URL getServiceUrl(String contextPath) throws Exception {
        return new URL(Boolean.getBoolean("harness.useSSL") ? "https" : "http", "localhost", httpPort, "/" + contextPath + "/");
    }

    private static int findFreePort(int x) {
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(x);
            return ss.getLocalPort();
        } catch (IOException ioe) {
        } finally {
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException ex) {
                    //ignore
                }
            }
        }
        return -1;
    }

    public static int getFreePort() {
        // set TCP port to somewhere between 20000-60000
        for (int i = 0; i < 10; i++) {
            int p = findFreePort(new SecureRandom().nextInt(40000) + 20000);
            if (p > 0) {
                return p;
            }
        }
        System.err.println("Couldn't find free port");
        return -1;
    }


}
