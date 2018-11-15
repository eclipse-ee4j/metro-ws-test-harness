/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test;

import com.sun.istack.test.VersionNumber;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;

/**
 * {@link OptionHandler} to process version number.
 * 
 * @author Kohsuke Kawaguchi
 */
public class VersionNumberHandler extends OptionHandler<VersionNumber> {
    public VersionNumberHandler(CmdLineParser parser, OptionDef option, Setter<? super VersionNumber> setter) {
        super(parser, option, setter);
    }

    public int parseArguments(Parameters params) throws CmdLineException {
        setter.addValue(new VersionNumber(params.getParameter(0)));
        return 1;
    }

    public String getDefaultMetaVariable() {
        return "VERSION";
    }
}
