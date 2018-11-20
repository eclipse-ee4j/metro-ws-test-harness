/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

/**
 * Container/transport abstraction API.
 *
 * <p>
 * This is the higher-most abstraction that represents the container
 * (which is an abstraction of the place where services are run.)
 *
 * <p>
 * Where the service is deployed is fundamentally dependent on
 * the transport to be tested. For example, testing HTTP transport
 * means deploying it on a web container or JavaEE container, while
 * testing the local transport would mean just running it inside
 * the same VM. Because this, interfaces in this package are implemented
 * by each transport.
 *
 * <p>
 * Since many transports would be ultimately deploying services
 * to a web container or a JavaEE container, we'll use Cargo
 * to further abstract away those containers.
 */
package com.sun.xml.ws.test.container;
