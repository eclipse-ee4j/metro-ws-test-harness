/*
 * Copyright (c) 2006, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package wsrm.secureroundtrip.server;

import jakarta.jws.WebService;

@WebService(endpointInterface="wsrm.secureroundtrip.server.IPingService")
public class IPingImpl {

    /**
     * @param String
     */
    public String ping(String message) {
        System.out.println("The message is here : " + message);
        return message;
    }
}
