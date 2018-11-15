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

/**
 * Ant task that invokes {@code APT} loaded in a separate classloader.
 *
 * @author Kohsuke Kawaguchi
 */
public final class AptTask extends AbstractJavacTask {
    public AptTask() {
        super("APT", AptAdapter.class);
    }

    public static final class AptAdapter extends JDKToolAdapter {
        protected String getMainMethod() {
            return "process";
        }

        protected String getMainClass() {
            return "com.sun.tools.apt.Main";
        }

        protected String getToolName() {
            return "APT";
        }
    }
}
