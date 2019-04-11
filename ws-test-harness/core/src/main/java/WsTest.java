/*
 * Copyright (c) 1997, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

/**
 * Convenient entry point that allows invocation like <code>"java WsTest ..."</code>,
 * assuming that everything is in the classpath.
 *
 * @author Kohsuke Kawaguchi
 */
public class WsTest {
    public static void main(String[] args) throws Exception {
        com.sun.xml.ws.test.Main.main(args);
    }
}
