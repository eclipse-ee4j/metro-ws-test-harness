/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.util;

import com.sun.istack.test.Which;
import com.sun.xml.ws.test.World;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapter;
import org.apache.tools.ant.taskdefs.compilers.DefaultCompilerAdapter;
import org.apache.tools.ant.types.Commandline;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link CompilerAdapter} that loads tools from the tools realm.
 *
 * <p>
 * The default adapter in Ant assumes that tools.jar is in the current classloader,
 * which is not the case here.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class JDKToolAdapter extends DefaultCompilerAdapter {
    /**
     * Run the compilation.
     *
     * @exception BuildException if the compilation has problems.
     */
    public boolean execute() throws BuildException {
        Commandline cmd = setupModernJavacCommand();

        try {
            int result = (Integer) getMain().invoke(null, (Object)cmd.getArguments());
            return result == 0;
        } catch (Exception ex) {
            throw new BuildException("Error compiling code", ex);
        }
    }

    private Method getMain() {
        try {
            Class<?> clazz = World.tool.getClassLoader()
                .loadClass(getMainClass());

            if(reported.add(clazz))
                // report where we loaded tools to assist classpath related issues
                System.out.println("Using "+getToolName()+" from "+ Which.which(clazz));

            return clazz.getMethod(getMainMethod(), String[].class);
        } catch( Throwable e ) {
            e.printStackTrace();
            throw new AssertionError("Unable to locate "+getToolName()+". Maybe you are using JRE?");
        }

    }

    protected abstract String getMainMethod();
    protected abstract String getMainClass();
    protected abstract String getToolName();

    private static Set<Class> reported = Collections.synchronizedSet(new HashSet<Class>());
}
