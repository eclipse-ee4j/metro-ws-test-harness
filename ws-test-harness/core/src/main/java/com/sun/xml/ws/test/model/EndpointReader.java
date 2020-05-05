/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import jakarta.jws.WebService;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import jakarta.xml.ws.WebServiceProvider;

/**
 *
 * @author lukas
 */
@SupportedAnnotationTypes({
    "jakarta.jws.WebService",
    "jakarta.xml.ws.WebServiceProvider"
})
public class EndpointReader extends AbstractProcessor {

    private Elements els;
    private final Set<TestEndpoint> endpoints = new LinkedHashSet<TestEndpoint>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        els = processingEnv.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        List<Element> classes = new ArrayList<Element>();
        for (Element e : roundEnv.getRootElements()) {
            if (e.getKind().equals(ElementKind.INTERFACE)) {
                continue;
            }
            classes.add(e);
        }
        Collections.sort(classes, new Comparator<Element>() {

            public int compare(Element o1, Element o2) {
                return getClassName(o1).compareTo(getClassName(o2));
            }
        });
        for (Element e : classes) {
            String serviceName = null;
            String portName = null;
//            String name = null;
//            String tns = null;
            String fullName = getClassName(e);
            WebService ws = e.getAnnotation(WebService.class);
            if (ws != null) {
                //SEI may not exist yet
                if (serviceName == null) {
                    serviceName = ws.serviceName().isEmpty() ? e.getSimpleName().toString() + "Service" : ws.serviceName();
                }
                if (portName == null) {
                    portName = ws.portName().isEmpty() ? null : ws.portName();
                }
            } else {
                WebServiceProvider wsp = e.getAnnotation(WebServiceProvider.class);
                if (wsp != null) {
                    if (serviceName == null) {
                        serviceName = wsp.serviceName().isEmpty() ? e.getSimpleName().toString() + "Service" : wsp.serviceName();
                    }
                    if (portName == null) {
                        portName = wsp.portName().isEmpty() ? null : wsp.portName();
                    }
                } else {
                    //not @WebService/Provider
                    continue;
                }
            }
            endpoints.add(new TestEndpoint(serviceName, fullName, portName, e.getAnnotation(WebServiceProvider.class) != null));
        }
        return true;
    }

    Set<TestEndpoint> getTestEndpoints() {
        return endpoints;
    }

    private String getPackageName(Element e) {
        String pkg = els.getPackageOf(e).getQualifiedName().toString();
        return pkg.isEmpty() ? "" : pkg + ".";
    }

    private String getClassName(Element e) {
        return getPackageName(e) + e.getSimpleName().toString();
    }
}
