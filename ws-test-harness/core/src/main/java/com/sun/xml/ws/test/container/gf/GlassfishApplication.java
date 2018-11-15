/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.container.gf;

import com.sun.xml.ws.test.container.AbstractHttpApplication;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.container.DeployedService;

import javax.enterprise.deploy.spi.TargetModuleID;
import java.net.URL;

/**
 * {@link Application} implementation for {@link GlassfishContainer}.
 *
 * @author Kohsuke Kawaguchi
 */
final class GlassfishApplication extends AbstractHttpApplication {

    private final GlassfishContainer container;

    /**
     * These JSR-88 objects represent the deployed modules.
     */
    private final TargetModuleID[] modules;

    public GlassfishApplication(URL warURL, DeployedService service, GlassfishContainer container, TargetModuleID[] modules) {
        super(warURL, service);
        this.container = container;
        this.modules = modules;
    }

    public void undeploy() throws Exception {
        container.undeploy(modules,warURL);
    }
}
