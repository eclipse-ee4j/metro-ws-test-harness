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
 * Code that executes tests based on the test model.
 *
 * <p>
 * {@link Executor}s are JUnit tests and
 * {@link TestDescriptor#build(ApplicationContainer, TestSuite)}
 * will put them in the right order in {@link TestSuite},
 * then calling {@link TestRunner#run(Test)} will execute those tests.
 */
package com.sun.xml.ws.test.exec;

import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.model.TestDescriptor;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
