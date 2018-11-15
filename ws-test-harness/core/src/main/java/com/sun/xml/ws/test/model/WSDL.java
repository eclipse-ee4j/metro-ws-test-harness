/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.model;

import com.sun.istack.Nullable;

import java.io.File;
import java.util.List;

/**
 * @author Bhakti Mehta
 */
public class WSDL {

    //Optional WSDL file that describes this service.
    @Nullable
    public final File wsdlFile;

    //Optional imported wsdls that are imported by the primary wsdl (wsdlFile)
    @Nullable
    public final List<File> importedWsdls;

    //Optional schema files that are imported by the wsdl
    @Nullable
    public final List<File> schemas;

    //Optional relative location of the WSDL (under WEB-INF/wsdl tree)
    @Nullable
    public final String relativeLocation;

    public WSDL(File wsdlFile, List<File> importedWsdls, List<File> schemafiles, String relativeLocation) {
        this.wsdlFile = wsdlFile;
        this.importedWsdls = importedWsdls;
        this.schemas = schemafiles;
        this.relativeLocation = relativeLocation;
    }

}
