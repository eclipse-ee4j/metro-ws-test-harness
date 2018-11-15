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

import com.sun.xml.ws.test.CodeGenerator;
import com.sun.xml.ws.test.container.DeploymentContext;

/**
 * {@link Executor} that cleans up the work directory (if necessary)
 * and create directories (if necessary).
 *
 * This is needed to avoid picking up old artifacts from the previous test run.
 *
 * @author Kohsuke Kawaguchi
 */
public class PrepareExecutor extends Executor {

    private final boolean clean;

    public PrepareExecutor(DeploymentContext context, boolean clean) {
        super(clean?"clean":"prepare", context);
        this.clean = clean;
    }

    public void runTest() throws Throwable {
        CodeGenerator.testStarting(context.workDir);
        context.prepare(clean);
    }
}
