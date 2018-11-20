/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.model;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;

/**
 * Endpoint exposed from {@link TestService}.
 *
 * @author Kohsuke Kawaguchi
 */
public class TestEndpoint {
    /**
     * Name of the endpoint.
     *
     * The name must be:
     * <ol>
     *  <li>Unique within {@link TestService}
     *  <li>a valid Java identifier.
     * </ol>
     *
     * <p>
     * This value is used to infer the port QName, the proxy object variable
     * name to be injected, etc.
     *
     * <p>
     * The endpoint will be deployed to "/[name]" URL.
     */
    @NotNull
    public final String name;

    /**
     * Name of the class that implements this endpoint.
     */
    @NotNull
    public final String className;

    /**
     * Encoded port name "{uri}local". This value is obtained from the @WebService/Provider class,
     * and may not be always available.
     */
    @Nullable
    public final String portName;

    /**
     * URL pattern like "/foo" where this service is bound.
     */
    @NotNull
    public String urlPattern;

    /**
     * If this class is WebServiceProvider and not WebService.
     */
    public final boolean isProvider;

    public TestEndpoint(String name, String className, String portName, boolean isProvider) {
        this.name = name;
        this.className = className;
        this.portName = portName;
        this.isProvider = isProvider;
    }
}
