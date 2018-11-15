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

import java.util.List;

/**
 * Ant task that invokes {@code Javac} loaded in a separate classloader.
 *
 * @author Kohsuke Kawaguchi
 */
public final class JavacTask extends AbstractJavacTask {

    public JavacTask() {
        super("javac", JavacAdapter.class);
    }

    public JavacTask(List<String> javacArguments) {
        this();
        addArguments(javacArguments);
    }

    public static final class JavacAdapter extends JDKToolAdapter {
        protected String getMainMethod() {
            return "compile";
        }

        protected String getMainClass() {
            return "com.sun.tools.javac.Main";
        }

        protected String getToolName() {
            return "javac";
        }
    }
}
