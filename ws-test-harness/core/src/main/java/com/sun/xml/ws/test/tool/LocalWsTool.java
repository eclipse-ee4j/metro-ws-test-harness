/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.tool;

import com.sun.istack.test.Which;
import com.sun.xml.ws.test.World;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@link WsTool} run locally within the same VM.
 * 
 * @author Kohsuke Kawaguchi
 */
final class LocalWsTool extends WsTool {

    private final Method main;

    public LocalWsTool(String className, boolean dumpParameters) {
        super(dumpParameters);
        try {
            Class wsimport = World.tool.loadClass(className);
            System.out.println("Using "+Which.which(wsimport));
            main = wsimport.getMethod("doMain",String[].class);
            Thread.currentThread().setContextClassLoader(World.tool.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new Error("Unable to find tool "+className,e);
        } catch (NoSuchMethodException e) {
            throw new Error("No main method on "+className,e);
        }
    }

    public void invoke(String... args) throws Exception {

        if (dumpParams()) {
            List<String> params = new ArrayList<String>();
            params.add(main.getDeclaringClass().getName() + "#" + main.getName() + "()");
            params.addAll(Arrays.asList(args));
            dumpWsParams(params);
        }

        int r = (Integer)main.invoke(null,new Object[]{args});
        if(r!=0)
            assertEquals("wsimport reported exit code "+r, 0,r);
    }
}
