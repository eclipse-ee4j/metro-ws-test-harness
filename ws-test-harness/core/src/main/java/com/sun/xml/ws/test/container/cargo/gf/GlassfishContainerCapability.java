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

import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.internal.J2EEContainerCapability;

/**
 * @author Kohsuke Kawaguchi
 */
final class GlassfishContainerCapability extends J2EEContainerCapability {
    public boolean supportsDeployableType(DeployableType type)
    {
        return (type == DeployableType.EJB) || super.supportsDeployableType(type);
    }
}
