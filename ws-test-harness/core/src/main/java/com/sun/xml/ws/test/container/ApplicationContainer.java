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

import java.util.Set;

/**
 * Represents a place that runs services.
 *
 * <p>
 * This object needs to be multi-thread safe.
 * 
 * @author Kohsuke Kawaguchi
 */
public interface ApplicationContainer {
    /**
     * Returns the transport that this container uses for testing.
     *
     * @return
     *      For example, "http", "local", "tcp", "jms", etc. It should match
     *      the scheme portion of
     *      the endpoint address URI. Never null. This value is compared in
     *      the descriptor's transport declaration to decide wheter to run a
     *      test or not.
     */
    String getTransport();

    /**
     * Returns the unsupported uses for this container. For e.g lwhs
     * container may return "servlet" as an unsupported "use"
     *
     * @return unsupported uses like "servlet", "nonapi" etc
     *      the returned value is compared againt descriptor's "uses" decl
     *      to decide wheter to run a test or not.
     */
    @NotNull Set<String> getUnsupportedUses();

    /**
     * Starts the container.
     *
     * This is invoked at the very beginning before
     * any service is deployed.
     */
    void start() throws Exception;

    /**
     * Starts a service inside a container, making it ready
     * to process requests.
     */
    @NotNull Application deploy(DeployedService service) throws Exception;

    /**
     * Stops the container.
     *
     * This is invoked at the end to clean up any resources.
     */
    void shutdown() throws Exception;
}
