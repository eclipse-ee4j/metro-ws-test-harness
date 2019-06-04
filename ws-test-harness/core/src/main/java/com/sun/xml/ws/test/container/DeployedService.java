/*
 * Copyright (c) 1997, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.container;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.model.TestService;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Information about running {@link TestService}.
 *
 * @author Kohsuke Kawaguchi
 */
public final class DeployedService {

    /**
     * The {@link DeploymentContext} that owns this service.
     */
    public final @NotNull DeploymentContext parent;

    /**
     * Service that was deployed.
     */
    public final @NotNull TestService service;

    /**
     * {@link Application} that represents the currently deployed service
     * on the container.
     *
     * <p>
     * This field is set when a service is deployed.
     */
    public Application app;

    /**
     * Root of the working directory to store things related to this service.
     */
    public final @NotNull File workDir;

    /**
     * Directory to store a war file image.
     */
    public final @NotNull File warDir;

    /**
     * Classpaths to load client artifacts for this service.
     */
    public final List<URL> clientClasspaths = new ArrayList<URL>();

    /**
     * The classes that represents the generated <code>Service</code> classes.
     *
     * This field is populated when the service is deployed
     * and client artifacts are generated.
     *
     * In fromjava tests with multiple <code>@WebService</code>, you may actually
     * get multiple service classes for one deployed service (argh!)
     */
    public final List<Class> serviceClass = new ArrayList<Class>();

    /*package*/ DeployedService(DeploymentContext parent, TestService service) {
        this.parent = parent;
        this.service = service;

        // create work directory
        String rel = "services";
        if(service.name.length()>0)
            rel += '/' + service.name;
        this.workDir = new File(parent.workDir,rel);

        this.warDir = new File(workDir,"war");
    }

    /**
     * Creates working directory
     */
    /*package*/ void prepare() {
        warDir.mkdirs();
    }

    public File getResources() {
        return parent.getResources();
    }
}
