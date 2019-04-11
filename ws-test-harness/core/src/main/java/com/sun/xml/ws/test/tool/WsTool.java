/*
 * Copyright (c) 1997, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.tool;

import com.sun.xml.ws.test.CodeGenerator;
import junit.framework.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface to the <code>wsimport</code> or <code>wsgen</code> command-line tool.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class WsTool extends Assert {

    private boolean dumpParameters;

    protected WsTool(boolean dumpParameters) {
        this.dumpParameters = dumpParameters;
    }

    /**
     * Invokes wsimport with the given arguments.
     *
     * @throws Exception
     *      if the compilation fails.
     *      alternatively, JUnit {@link Assert}ions can be used
     *      to detect error conditions.
     */
    public abstract void invoke(String... args) throws Exception;

    /**
     * Returns true if the '-skip' mode is on and
     * the tool invocation is skipped.
     */
    public boolean isNoop() {
        return this==NOOP;
    }

    /**
     * Determines which compiler to use.
     *
     * @param externalWsImport
     *      null to run {@link com.sun.xml.ws.test.tool.WsTool} from {@link com.sun.xml.ws.test.World#tool}.
     *      Otherwise this file will be the path to the script.
     * @param extraWsToolsArgs
     */
    public static WsTool createWsImport(File externalWsImport, boolean dumpParameters, String extraWsToolsArgs) {
        return createTool(externalWsImport, "com.sun.tools.ws.WsImport", dumpParameters, extraWsToolsArgs);
    }

    /**
     * Determines which wsgen to use.
     *
     * @param externalWsGen
     *      null to run {@link com.sun.xml.ws.test.tool.WsTool} from {@link com.sun.xml.ws.test.World#tool}.
     *      Otherwise this file will be the path to the script.
     * @param extraWsToolsArgs
     */
    public static WsTool createWsGen(File externalWsGen, boolean dumpParameters, String extraWsToolsArgs) {
        return createTool(externalWsGen,"com.sun.tools.ws.WsGen", dumpParameters, extraWsToolsArgs);
    }

    private static WsTool createTool(File externalExecutable, String className, boolean dumpParameters, String extraWsToolsArgs) {
        if(externalExecutable !=null) {
            return new RemoteWsTool(externalExecutable, dumpParameters, extraWsToolsArgs);
        } else {
            return new LocalWsTool(className, dumpParameters);
        }
    }

    /**
     * {@link WsTool} that does nothing.
     *
     * <p>
     * This assumes that files that are supposed to be generated
     * are already generated.
     */
    public static WsTool NOOP = new WsTool(false) {
        public void invoke(String... args) {
        }
    };

    protected void dumpWsParams(List<String> params) {
        System.err.println("\n\nINVOKING WS Tool:\n");
        for(int i = 0; i < params.size(); i++) {
            System.err.print(i == 0? " " : "     ");
            System.err.print(params.get(i));
            System.err.println(i + 1 < params.size()? " \\" : "\n");
        }

        // generate ws script
        if (CodeGenerator.isGenerateTestSources()) {
            List mkdirs = new ArrayList();
            List params2 = new ArrayList();
            for (int i = 0; i < params.size(); i++) {
                String p = params.get(i);
                if (i == 0) {
                    int index = p.lastIndexOf("/");
                    index++;
                    if (index > 0 && index < p.length()) {
                        // remove full path from tool
                        p = p.substring(index);
                    }
                }
                if ("-s".equals(p.trim()) || "-d".equals(p.trim()) || "-r".equals(p.trim())) {
                    String dir = params.get(i + 1);
                    mkdirs.add(dir);
                }
                p = CodeGenerator.fixedURLBASH(p);

                // change absolute path to relative
                p = CodeGenerator.toRelativePath(p);

                // handle inner classes
                if (!p.startsWith("http://")) {
                    p = p.replaceAll("\\$", "\\\\\\$");
                }

                // ugly - "move" source wsdl to no-harness/ .. / .. /src dir
                if (p.startsWith("../") && !p.startsWith("../src")) {
                    p = p.replaceAll("\\.\\./", "../src/");
                }

                params2.add(p.replaceAll("localhost", "127.0.0.1"));
            }
            CodeGenerator.generateTool(mkdirs, params2);
        }
    }

    protected boolean dumpParams() {
        return dumpParameters || CodeGenerator.isGenerateTestSources();
    }

}
