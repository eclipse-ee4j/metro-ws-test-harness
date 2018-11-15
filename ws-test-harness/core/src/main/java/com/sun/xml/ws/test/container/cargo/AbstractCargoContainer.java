/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.container.cargo;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.container.AbstractApplicationContainer;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.tool.WsTool;
import org.codehaus.cargo.container.Container;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.deployer.Deployer;
import org.codehaus.cargo.container.deployer.DeployerType;
import org.codehaus.cargo.container.deployer.URLDeployableMonitor;
import org.codehaus.cargo.generic.AbstractFactoryRegistry;
import org.codehaus.cargo.generic.deployable.DefaultDeployableFactory;
import org.codehaus.cargo.generic.deployer.DefaultDeployerFactory;

import java.io.File;
import java.net.URL;

/**
 * @author Kohsuke Kawaguchi
 */
abstract class AbstractCargoContainer<C extends Container> extends AbstractApplicationContainer {

    private static final long deploymentTimeout = Long.valueOf(System.getProperty("harness.deploy.timeout", "30000"));
    /**
     * Expected to be set by the constructor of the derived class.
     * Conceptually final --- no update after that.
     */
    protected C container;

    protected final DefaultDeployerFactory deployerFactory = new DefaultDeployerFactory(AbstractFactoryRegistry.class.getClassLoader());
    protected final DefaultDeployableFactory deployableFactory = new DefaultDeployableFactory(AbstractFactoryRegistry.class.getClassLoader());


    protected AbstractCargoContainer(WsTool wsimport, WsTool wsgen, boolean httpspi) {
        super(wsimport, wsgen, httpspi);
    }

    public String getTransport() {
        return "http";
    }

    @NotNull
    public Application deploy(DeployedService service) throws Exception {
        String contextPath = service.service.getGlobalUniqueName();
        File archive;

        if(needsArchive()) {
            archive = new File(service.workDir,contextPath+".war");
            createWARZip(service,archive);
        } else {
            archive = assembleWar(service).root;
        }

        WAR war = (WAR)deployableFactory.createDeployable(
            container.getId(), archive.getAbsolutePath(), DeployableType.WAR);

        war.setContext(contextPath);

        Deployer deployer = deployerFactory.createDeployer(container, DeployerType.toType(container.getType()));

        URL serviceUrl = getServiceUrl(contextPath);

        System.out.println("Verifying that "+serviceUrl+" is already removed");
        try {
            deployer.undeploy(war);
        } catch (Exception e) {
            // swallow any failure to undeploy
        }
        System.out.println("Deploying a service to "+serviceUrl);
//        System.out.println("Timeout " + deploymentTimeout + "ms");
        deployer.deploy(war,new URLDeployableMonitor(serviceUrl, deploymentTimeout));

        return new CargoApplication( deployer, war, serviceUrl, service);
    }

    protected abstract URL getServiceUrl(String contextPath) throws Exception;

    /**
     * True if the Cargo implementation only takes a .war file
     * and not the exploded war image.
     *
     * Not creating a war file makes the testing faster.
     */
    protected boolean needsArchive() {
        return true;
    }
}
