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
import com.sun.xml.ws.test.World;
import com.sun.xml.ws.test.container.DeploymentContext;
import com.sun.xml.ws.test.util.JavacTask;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Used to compile clients when there's no server to deploy.
 *
 * @author Kohsuke Kawaguchi
 */
public class ClientCompileExecutor extends Executor {
    public ClientCompileExecutor(DeploymentContext context) {
        super("Compile clients "+context.descriptor.name, context);
    }

    public void runTest() throws Throwable {
        CodeGenerator.testStarting(context.workDir);
        File classDir = makeWorkDir("client-classes");

        // compile the generated source files to javac
        JavacTask javac = new JavacTask(context.descriptor.javacOptions);

        javac.setSourceDir(
            context.descriptor.common,
            new File(context.descriptor.home,"client")
        );
        javac.setDestdir(classDir);
        javac.setDebug(true);
        if(!context.wsimport.isNoop()) {
            // if we are just reusing the existing artifacts, no need to recompile.
            javac.execute();
            CodeGenerator.generateJavac(javac);
        }

        // load the generated classes and resources
        URL[] url = (context.getResources() == null)
                ? new URL[] {classDir.toURL()}
                : new URL[] {classDir.toURL(),context.getResources().toURL()};
        ClassLoader cl = new URLClassLoader( url, World.runtime.getClassLoader() );

        context.clientClassLoader = cl;
    }
}
