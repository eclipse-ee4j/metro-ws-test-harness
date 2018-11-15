/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test;

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;

@Retention(value= RetentionPolicy.RUNTIME)
@Target({TYPE})
/**
 * This captures the Version requirements for the Java based test (junit test).
 * This is added in bootstrap such that the harness based tests can easily depend on it without it just like any other
 * junit classes. 
 *
 * @author Rama Pulavarthi
 */

public @interface VersionRequirement {
    String since() default "";
    String until() default "";
    String exlcudeFrom() default "";
}
