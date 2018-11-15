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

/**
 * How we use wsgen.
 *
 * @author Kohsuke Kawaguchi
 */
public enum WsGenMode {
    /**
     * Invokes wsgen for eager wrapper bean generation.
     * This is the default.
     */
    ALWAYS,
    /**
     * Don't generate wrapper beans (the harness still actually executes wsgen
     * for its own purpose but the wrapper beans will be discarded.)
     */
    IGNORE,
    /**
     * Test both scenarios.
     */
    BOTH,
}
