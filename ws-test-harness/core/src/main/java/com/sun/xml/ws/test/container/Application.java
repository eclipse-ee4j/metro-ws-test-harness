/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.container;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.model.TestEndpoint;

import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 * Represents an application deployed inside a {@link ApplicationContainer}.
 *
 * <p>
 * This object needs to be multi-thread safe.
 *
 * @author Kohsuke Kawaguchi
 */
public interface Application {
    /**
     * Returns the actual endpoint address to which the given {@link TestEndpoint}
     * is deployed.
     */
    @NotNull URI getEndpointAddress(@NotNull TestEndpoint endpoint) throws Exception;

    /**
     * Gets the WSDL of this service.
     *
     * <p>
     * This WSDL will be compiled to generate client artifacts during a test.
     * In the general case, you may get more than one WSDL from one web application.
     */
    @NotNull
    List<URL> getWSDL() throws Exception;

    /**
     * Removes this application from the container.
     */
    void undeploy() throws Exception;
}
