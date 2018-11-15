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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Launches {@code wsimport} as a separate process.
 *
 * @author Kohsuke Kawaguchi
 */
final class RemoteWsTool extends WsTool {

    /**
     * The path to the executable tool.bat or wsimport.sh
     */
    private final File executable;

    private String toolsExtraArgs = null;


    public RemoteWsTool(File executable, boolean dumpParameters, String toolsExtraArgs) {
        super(dumpParameters);
        this.executable = executable;
        this.toolsExtraArgs = toolsExtraArgs;
        if(!executable.exists())
            throw new IllegalArgumentException("Non-existent executable "+executable);
    }

    public void invoke(String... args) throws Exception {
        List<String> params = new ArrayList<String>();
        params.add(executable.getPath());

        // add http proxy properties as CLI arguments
        String proxyHost = System.getProperty("http.proxyHost");
        if (proxyHost != null) {
            params.add("-J-Dhttp.proxyHost="+proxyHost);
        }
        String proxyPort = System.getProperty("http.proxyPort");
        if (proxyPort != null) {
            params.add("-J-Dhttp.proxyPort="+proxyPort);
        }
        String nonProxyHosts = System.getProperty("http.nonProxyHosts");
        if (nonProxyHosts != null) {
            // if running in bash, value must be quoted
            if (!nonProxyHosts.startsWith("\"")) {
                nonProxyHosts = "\"" + nonProxyHosts + "\"";
            }
            params.add("-J-Dhttp.nonProxyHosts="+nonProxyHosts);
        }
        if (toolsExtraArgs != null) {
            System.err.println("adding extra tools args [" + toolsExtraArgs + "]");
            params.add(toolsExtraArgs);
        }


        params.addAll(Arrays.asList(args));

        if (dumpParams()) {
            dumpWsParams(params);
        }

        ProcessBuilder b = new ProcessBuilder(params);
        b.redirectErrorStream(true);
        Process proc = b.start();

        // copy the stream and wait for the process completion
        proc.getOutputStream().close();
        byte[] buf = new byte[8192];
        InputStream in = proc.getInputStream();
        int len;
        while((len=in.read(buf))>=0) {
            System.out.write(buf,0,len);
        }

        int exit = proc.waitFor();
        assertEquals(
            "wsimport reported exit code "+exit,
            0,exit);
    }

}
