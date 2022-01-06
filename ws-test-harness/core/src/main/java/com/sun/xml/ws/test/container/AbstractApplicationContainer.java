/*
 * Copyright (c) 1997, 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.container;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.World;
import com.sun.xml.ws.test.container.jelly.EndpointInfoBean;
import com.sun.xml.ws.test.model.TestService;
import com.sun.xml.ws.test.tool.WsTool;
import com.sun.xml.ws.test.util.WSITUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base implementation of {@link ApplicationContainer}.
 * <p>
 * This implementation provides code for common tasks, such as assembling files
 * into a war, etc.
 *
 * @author Kohsuke Kawaguchi
 * @author Ken Hofsass
 */
public abstract class AbstractApplicationContainer implements ApplicationContainer {
    private final WsTool wsimport;
    private final WsTool wsgen;
    private final Set<String> unsupportedUses;
    private final boolean httpspi;

    protected AbstractApplicationContainer(WsTool wsimport, WsTool wsgen, boolean httpspi) {
        this.wsimport = wsimport;
        this.wsgen = wsgen;
        this.unsupportedUses = new HashSet<String>();
        this.httpspi = httpspi;
    }

    @NotNull
    public Set<String> getUnsupportedUses() {
        return unsupportedUses;
    }

    /**
     * Prepares an exploded war file image for this service.
     */
    protected WAR assembleWar(DeployedService service) throws Exception {
        WAR war = new WAR(service);

        boolean fromJava = service.service.wsdl.isEmpty();

        if (!fromJava) {
            war.compileWSDL(wsimport);
        }

        if (!isSkipMode()) {
            war.compileJavac();
        }

        // copy external metadata files, if any ....
        File[] externalMetadataFiles = getExternalMetadataFiles(service.service);
        if (externalMetadataFiles != null) {
            war.copyToClasses(externalMetadataFiles);
            updateWsgenOpts(service.service);
        }

        if (fromJava) {
            war.generateWSDL(wsgen);
        }

        if (!isSkipMode()) {

            // search for endpoints only in case we need to generate any descriptor ...
            List<EndpointInfoBean> endpoints = null;

            // we only need this for Glassfish, but it's harmless to generate for other containers.
            // TODO: figure out how not to do this for other containers
            File configuredSunJaxwsXml = service.service.getConfiguredFile("sun-jaxws.xml");
            if (configuredSunJaxwsXml == null) {
                endpoints = war.getEndpointsInfos();
                war.generateSunJaxWsXml(endpoints);
            } else {
                war.copyToWEBINF(configuredSunJaxwsXml);
            }

            File configuredWebXml = service.service.getConfiguredFile("web.xml");
            if (configuredWebXml == null) {
                if (endpoints == null) {
                    endpoints = war.getEndpointsInfos();
                }
                war.generateWebXml(endpoints, httpspi);
            } else {
                war.copyToWEBINF(configuredWebXml);
            }

            PrintWriter w = new PrintWriter(new FileWriter(new File(war.root, "index.html")));
            w.println("<html><body>Deployed by the JAX-WS test harness</body></html>");
            w.close();

            // we only need this for embedded Tomcat, but it's harmless to generate for other containers.
            // TODO: figure out how not to do this for other containers
            File metainf = new File(war.root,"META-INF");
            metainf.mkdirs();
            w = new PrintWriter(new FileWriter(new File(metainf, "context.xml")));
            //avoid default scanning of Class-Path in manifest to get rif of warnings during test run
            w.println("<Context><JarScanner scanManifest=\"false\"/></Context>");
            w.close();
        }
        //Package Handler Configuration files
        war.copyToClasses(service.service.getHandlerConfiguration());

        //copy resources to amke it available on classpath.
        war.copyResources(service.getResources());
        return war;
    }

    protected void updateWsgenOpts(TestService service) {

        if (service.parent.metadatafiles != null) {
            for (String path : service.parent.metadatafiles) {
                service.parent.wsgenOptions.add("-x");
                service.parent.wsgenOptions.add(path);
            }
        }
    }

    protected void updateWsitClient(WAR war, DeployedService deployedService, String id) throws Exception {
        File wsitClientFile = new File(deployedService.getResources(), "wsit-client.xml");
        if (wsitClientFile.exists()) {
            WSITUtil.updateWsitClient(wsitClientFile, id, deployedService.service.wsdl.get(0).wsdlFile.toURI().toString());
            war.copyWsit(wsitClientFile);
        } else {
            throw new RuntimeException("wsit-client.xml is absent. It is required. \n"
                    + "Please check " + deployedService.getResources());
        }
    }

    private File[] getExternalMetadataFiles(TestService service) {
        List<File> files = null;
        if (service.parent.metadatafiles != null) {
            for (String path : service.parent.metadatafiles) {
                if (files == null) {
                    files = new ArrayList<File>();
                }
                files.add(new File(service.getAbsolutePath(path)));
            }
        }
        return files == null ? null : files.toArray(new File[files.size()]);
    }

    /**
     * Returns true if we are running with the "-skip" option,
     * where we shouldn't generate any artifacts and just pick up the results from the last run.
     */
    private boolean isSkipMode() {
        return wsgen.isNoop() && wsimport.isNoop();
    }

    /**
     * Prepares a fully packaged war file to the specified location.
     */
    protected final WAR createWARZip(DeployedService service, File archive) throws Exception {
        WAR assembly = assembleWar(service);

        // copy runtime classes into the classpath. this is slow.
        // isn't there a better way to do this?
        if (copyRuntimeLibraries()) {
            System.out.println("Copying runtime libraries");
            assembly.copyClasspath(World.runtime);
        }

        System.out.println("Assembling a war file");
        assembly.zipTo(archive);

        return assembly;
    }

    /**
     * Copy JAX-WS runtime code?
     */
    protected boolean copyRuntimeLibraries() {
        return false;
    }
}
