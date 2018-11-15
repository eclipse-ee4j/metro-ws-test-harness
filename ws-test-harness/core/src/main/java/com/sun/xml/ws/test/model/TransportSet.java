/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.model;

/**
 * Set of transports.
 *
 * @author Kohsuke Kawaguchi
 */
public interface TransportSet {
    /**
     * Checks if the given transport is contained in this set.
     */
    boolean contains(String transport);

    /**
     * Constant that represents a set that includes everything.
     */
    public static final TransportSet ALL = new TransportSet() {
        public boolean contains(String transport) {
            return true;
        }
    };

    /**
     * {@link TransportSet} that consists of a single value.
     */
    public static final class Singleton implements TransportSet {
        private final String transport;

        public Singleton(String transport) {
            this.transport = transport;
        }

        public boolean contains(String transport) {
            return this.transport.equals(transport);
        }
    }
}
