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

import com.sun.xml.ws.test.World;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapter;
import org.apache.tools.ant.types.Path;

import java.io.File;
import java.util.List;

public abstract class AbstractJavacTask extends Javac {

    AbstractJavacTask(String toolName, Class<? extends CompilerAdapter> adapterClass) {
        setProject(World.project);
        setTaskType(toolName);
        setTaskName(toolName);
        setCompiler(adapterClass.getName());
        setClasspath(World.runtime.getPath());
    }

    public void addArguments(List<String> arguments) {
        addArguments(arguments.toArray(new String[arguments.size()]));
    }

    public void addArguments(String ... arguments) {
        for (String argument : arguments) {
            ImplementationSpecificArgument arg = createCompilerArg();
            arg.setLine(argument);
        }
    }

    /**
     * Set the source directories where java files
     * to be compiled are located.
     */
    public void setSourceDir(File... sourceDirs) {
        Path path = new Path(getProject());
        for (File sourceDir : sourceDirs) {
            if (sourceDir == null) continue;
            if (!sourceDir.exists()) continue;
            path.createPathElement().setPath(sourceDir.getAbsolutePath());
        }
        setSrcdir(path);
    }
}
