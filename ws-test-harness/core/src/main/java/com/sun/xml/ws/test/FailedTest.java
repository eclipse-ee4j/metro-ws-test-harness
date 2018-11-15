/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test;

import junit.framework.TestCase;

import java.io.File;

/**
 * A test case that will always fail.
 *
 * @author Kohsuke Kawaguchi
 */
class FailedTest extends TestCase {
    private final Throwable t;

    public FailedTest(String name, Throwable t) {
        super(name);
        this.t = t;
    }

// this turns out to be a bad idea because in this way the full path name will appear as the test suite name,
// which messes up <junitreport> because it tries to use that as the package name. 
//    public FailedTest(File f, Throwable t) {
//        this(f.getPath(),t);
//    }

    protected void runTest() throws Throwable {
        throw new Exception(t);
    }
}
