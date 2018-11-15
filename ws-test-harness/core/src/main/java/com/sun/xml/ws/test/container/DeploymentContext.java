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
import com.sun.xml.ws.test.model.TestDescriptor;
import com.sun.xml.ws.test.model.TestService;
import com.sun.xml.ws.test.tool.WsTool;
import com.sun.xml.ws.test.util.FileUtil;
import junit.framework.TestCase;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * State of running {@link TestDescriptor} execution.
 *
 * {@link TestCase}s that work on the same {@link TestDescriptor}
 * shares this object to communicate information between them.
 *
 * @author Kohsuke Kawaguchi
 */
public class DeploymentContext {

    /**
     * The test descriptor that governs all the deployed services.
     */
    public final @NotNull TestDescriptor descriptor;

    /**
     * Container where services are deployed.
     */
    public final @NotNull ApplicationContainer container;

    /**
     * {@link WsTool} tool to be used.
     */
    public final @NotNull WsTool wsimport;

    /**
     * Which service is deployed where?
     */
    public final Map<TestService,DeployedService> services;

    /**
     * {@link ClassLoader} that loads all the generated client artifacts.
     * This is used to run client tests.
     */
    public ClassLoader clientClassLoader;

    /**
     * Work directory top.
     *
     * <p>
     * During test execution, this directory can be used as a temporary directory
     * to store various temporary artifacts.
     *
     * <p>
     * If you store something in the working directory, be sure to first create
     * a sub-directory, to avoid colliding with other parts of the harness
     * that uses the work directory.
     */
    public final File workDir;

    private File resources = null;

    public DeploymentContext(TestDescriptor descriptor, ApplicationContainer container, WsTool wsimport) {
        this.descriptor = descriptor;
        this.container = container;
        this.wsimport = wsimport;

        // create workspace
        this.workDir = new File(descriptor.home,"work");

        // create DeployedService objects
        Map<TestService,DeployedService> services = new HashMap<TestService, DeployedService>();
        for (TestService svc : descriptor.services) {
            services.put(svc, new DeployedService(this,svc));
        }
        this.services = Collections.unmodifiableMap(services);
    }

    /**
     * Creates working directories.
     *
     * @param clean
     *      if true, all the previous left-over files in the working directory
     *      will be deleted.
     */
    public void prepare(boolean clean) {
        if(clean) {
            FileUtil.deleteRecursive(workDir);
        }
        workDir.mkdirs();
        if (descriptor.resources != null) {
            resources = new File(workDir, "resources");
            FileUtil.copyDir(descriptor.resources, resources, null);
        }

        for (DeployedService ds : services.values()) {
            ds.prepare();
        }
    }

    public File getResources() {
        return resources;
    }
}
