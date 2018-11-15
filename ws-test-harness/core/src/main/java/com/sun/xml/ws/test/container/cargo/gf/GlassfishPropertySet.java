/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.container.cargo.gf;

/**
 * Interface for Glassfish-specific properties.
 *
 */
public interface GlassfishPropertySet
{
    /**
     * The admin HTTP port that Glassfish will use.
     * Defaults to 4848.
     */
    String ADMIN_PORT = "cargo.glassfish.adminPort";

//
// these names are named to match asadmin --domainproperties option
//

    /**
     * JMS port. Defaults to 7676.
     */
    String JMS_PORT = "cargo.glassfish.jms.port";

    /**
     * IIOP port. Defaults to 3700.
     */
    String IIOP_PORT = "cargo.glassfish.orb.listener.port";

    /**
     * HTTPS port. Defaults to 8181.
     */
    String HTTPS_PORT = "cargo.glassfish.http.ssl.port";

    /**
     * IIOP+SSL port. Defaults to 3820.
     */
    String IIOPS_PORT = "cargo.glassfish.orb.ssl.port";

    /**
     * IIOP mutual authentication port. Defaults to 3920.
     */
    String IIOP_MUTUAL_AUTH_PORT = "cargo.glassfish.orb.mutualauth.port";

    /**
     * JMX admin port. Defaults to 8686.
     */
    String JMX_ADMIN_PORT = "cargo.glassfish.domain.jmxPort";
}
