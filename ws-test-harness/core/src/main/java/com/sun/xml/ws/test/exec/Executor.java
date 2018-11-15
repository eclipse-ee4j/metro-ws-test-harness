/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.exec;

import com.sun.xml.ws.test.container.DeploymentContext;
import junit.framework.TestCase;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Executes a part of a test.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class Executor extends TestCase {
    /**
     * Every {@link Executor} works for one {@link DeploymentContext}.
     */
    public final DeploymentContext context;

    /**
     * Map to store changed system properties - for future restore
     */
    private Map<String, String> systemProperties;

    @Override
    protected void setUp() throws Exception {
        setSystemProperties(context.descriptor.systemProperties);
    }

    @Override
    protected void tearDown() throws Exception {
        rollBackSystemProperties();
    }

    protected Executor(String name, DeploymentContext context) {
        super(context.descriptor.name+"."+name);
        this.context = context;
    }

    /**
     * Executes something.
     *
     * Error happened during this will be recorded as a test failure.
     */
    @Override
    public abstract void runTest() throws Throwable;

    protected final File makeWorkDir(String dirName) {
        File gensrcDir = new File(context.workDir, dirName);
        gensrcDir.mkdirs();
        return gensrcDir;
    }

    /**
     * Setting up required for test system properties
     * @param propertiesList list of properties
     */
    final void setSystemProperties(Collection<String> propertiesList) {
        systemProperties = new HashMap<String, String>();

        for (String property : propertiesList) {
            String[] parts = property.split("=");
            // **** validation ********
            if (parts.length != 2)
                throw new IllegalArgumentException("Bad system property: " + property);
            String name = parts[0];
            String newValue = parts[1];
            if (name == null || name.isEmpty() || newValue == null || newValue.isEmpty())
                throw new IllegalArgumentException("Bad system property: " + property);
            // ************

            String currentValue = System.getProperty(name);
            systemProperties.put(name, currentValue);
            System.setProperty(name, newValue);
        }
    }

    /**
     * Restoring original system properties
     */
    final void rollBackSystemProperties() {
        for (String name : systemProperties.keySet()) {

            String value = systemProperties.get(name);
            if (value != null)
                System.setProperty(name, value);
            else
                System.clearProperty(name);
        }
    }
}
