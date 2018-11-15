/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.exec;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.NameSpace;
import bsh.TargetError;
import com.sun.xml.ws.test.client.InterpreterEx;
import com.sun.xml.ws.test.client.ScriptBaseClass;
import com.sun.xml.ws.test.client.XmlResource;
import com.sun.xml.ws.test.CodeGenerator;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.container.DeploymentContext;
import com.sun.xml.ws.test.model.TestClient;
import com.sun.xml.ws.test.model.TestEndpoint;
import com.sun.xml.ws.test.World;

import java.beans.Introspector;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Executes {@link TestClient}.
 *
 * @author Kohsuke Kawaguchi
 */
public class ClientExecutor extends Executor {
    /**
     * Client test scenario to execute.
     */
    private final TestClient client;
    private List<String> pImports = new ArrayList<String>();
    private StringBuilder pContents = new StringBuilder();
    private Map<String, String> varMap = new HashMap<String, String>();

    public ClientExecutor(DeploymentContext context, TestClient client) {
        super("client " + client.script.getName().replace('.', '_'), context);
        this.client = client;
    }

    public void runTest() throws Throwable {
        CodeGenerator.testStarting(context.workDir);
        if (context.clientClassLoader == null) {
            context.clientClassLoader = context.getResources() != null
                    ? new URLClassLoader(new URL[]{context.getResources().toURL()}, World.runtime.getClassLoader())
                    : World.runtime.getClassLoader();
        }

        Interpreter engine = new InterpreterEx(context.clientClassLoader);

        NameSpace ns = engine.getNameSpace();
        // import namespaces. what are the other namespaces to be imported?
        importPackage(ns, "javax.activation");
        importPackage(ns, "javax.xml.ws");
        importPackage(ns, "javax.xml.ws.soap");
        importPackage(ns, "javax.xml.ws.handler");
        importPackage(ns, "javax.xml.ws.handler.soap");
        importPackage(ns, "javax.xml.bind");
        importPackage(ns, "javax.xml.soap");
        importPackage(ns, "javax.xml.namespace");
        importPackage(ns, "javax.xml.transform");
        importPackage(ns, "javax.xml.transform.sax");
        importPackage(ns, "javax.xml.transform.dom");
        importPackage(ns, "javax.xml.transform.stream");
        importPackage(ns, "java.util");
        importPackage(ns, "java.util.concurrent");
        // if there's any client package, put them there
        importPackage(ns, context.descriptor.name + ".client");
        if (context.descriptor.common != null && context.descriptor.common.exists()) {
            importPackage(ns, context.descriptor.name + ".common");
        }

        // this will make 'thisObject' available as 'this' in script
        ns.importObject(new ScriptBaseClass(context, engine, client));

        // load additional helper methods
        try {
            engine.eval(new InputStreamReader(getClass().getResourceAsStream("util.bsh")));
        } catch (EvalError evalError) {
            throw new Error("Failed to evaluate util.bsh", evalError);
        }

        // when invoking JAX-WS, we need to set the context classloader accordingly
        // so that it can discover classes from the right places.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(context.clientClassLoader);

        try {
            injectResources(ns, engine);
            invoke(engine);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    protected void importPackage(NameSpace ns, String p) {
        ns.importPackage(p);
        pImports.add(p + ".*");
    }

    /**
     * Makes the test script invocation.
     */
    protected void invoke(Interpreter engine) throws Throwable {
        // executes the script
        Reader r = client.script.read();
        try {
            if (client.parent.setUpScript != null) {
                engine.eval(new StringReader(client.parent.setUpScript), engine.getNameSpace(), "pre-client script");
                varMap.put("client_setUp_script", client.parent.setUpScript + "\n");
            } else {
                varMap.put("client_setUp_script", "");
            }

            CodeGenerator.generateClientClass(
                    client.script.getName(), // test name
                    pImports,
                    client.script.getSource(),
                    varMap);

            engine.eval(r, engine.getNameSpace(), client.script.getName());

        } catch (TargetError e) {
            throw e.getTarget();
        } catch (Throwable t) {
            System.err.println("Caught throwable while evaluating script: [\n" + client.script.getSource() + "\n]");
            t.printStackTrace();
            throw t;
        } finally {
            r.close();
        }
    }

    private void injectResources(NameSpace ns, Interpreter engine) throws Exception {
        StringBuilder serviceList = new StringBuilder("injected services:");
        StringBuilder portList = new StringBuilder("injected ports:");
        StringBuilder addressList = new StringBuilder("injected addresses:");

        // inject test home directory
        engine.set("home", client.parent.home);
        varMap.put("home", client.parent.home.toString());

        // inject XML resources
        for (Entry<String, XmlResource> e : context.descriptor.xmlResources.entrySet())
            engine.set(e.getKey(), e.getValue());

        for (DeployedService svc : context.services.values()) {
            if (!svc.service.isSTS) {
                // inject WSDL URLs
                engine.set("wsdlUrls", svc.app.getWSDL());

                /*
                 TODO: some more work here
                // Server side may be provider endpoint that doesn't expose WSDL
                // So there are no generated services.
                if (svc.serviceClass.size() == 0) {

                    String portName = null;
                    for (TestEndpoint e : svc.service.endpoints) {
                        portName = e.portName;
                        break;
                    }
                    String varName = Introspector.decapitalize(portName);
                    engine.set(varName +"Address",svc.app.getEndpointAddress(getEndpoint(svc, portName)));
                    addressList.append(' ').append(varName).append("Address");
                    continue;
                }
                */

                for (Class clazz : svc.serviceClass) {
                    String packageName = clazz.getPackage().getName();
                    //  import the artifact package
                    ns.importPackage(packageName);
                    //  use reflection to list up all methods with 'javax.xml.ws.WebEndpoint' annotations
                    //  invoke that method via reflection to obtain the Port object.
                    //  set the endpoint address to that port object
                    //  inject it to the scripting engine
                    Method[] methods = clazz.getMethods();

                    // annotation that serviceClass loads and annotation that this code
                    // uses might be different
                    Class<? extends Annotation> webendpointAnnotation = clazz.getClassLoader()
                            .loadClass("javax.xml.ws.WebEndpoint").asSubclass(Annotation.class);
                    Method nameMethod = webendpointAnnotation.getDeclaredMethod("name");

                    Object serviceInstance = clazz.newInstance();

                    //{// inject a service instance
                    String serviceVarName = Introspector.decapitalize(clazz.getSimpleName());
                    engine.set(serviceVarName, serviceInstance);
                    varMap.put("serviceClassName", clazz.getName().replaceAll("\\$", "."));
                    varMap.put("serviceVarName", serviceVarName);
                    serviceList.append(' ').append(serviceVarName);
                    //}

                    for (Method method : methods) {
                        Annotation endpoint = method.getAnnotation(webendpointAnnotation);
                        // don't inject variables for methods like getHelloPort(WebServiceFeatures)
                        if (endpoint != null && method.getParameterTypes().length == 0) {

                            //For multiple endpoints the convention for injecting the variables is
                            // portName obtained from the WebEndpoint annotation,
                            // which would be something like "addNumbersPort"
                            String portName = (String) nameMethod.invoke(endpoint);
                            String varName = Introspector.decapitalize(portName);

                            try {
                                engine.set(varName, method.invoke(serviceInstance));
                                String portType = method.getReturnType().getName();

                                varMap.put("portType", portType);
                                varMap.put("getPortMethod", method.getName());
                                varMap.put("varName", varName);
                                varMap.put("serviceName", serviceVarName);
                                varMap.put("portName", portName);
                                String endpointAddress = svc.app.getEndpointAddress(getEndpoint(svc, portName)).toString();
                                engine.set("" + varName + "Address", endpointAddress);
                                addressList.append(' ').append(varName).append("Address");
                                varMap.put("address", endpointAddress);
                            } catch (InvocationTargetException e) {
                                if (e.getCause() instanceof Exception)
                                    throw (Exception) e.getCause();
                                else
                                    throw e;
                            }
                            portList.append(' ').append(varName);
                        }
                    }
                }
            }
        }
        System.out.println(serviceList);
        System.out.println(portList);
        System.out.println(addressList);
    }

    private TestEndpoint getEndpoint(DeployedService svc, String portName) {
        // first, look for the port name match
        for (TestEndpoint e : svc.service.endpoints) {
            if (e.portName != null && e.portName.equals(portName))
                return e;
        }
        // nothing explicitly matched.
        if (svc.service.endpoints.size() != 1)
            throw new Error("Multiple ports are defined on '" + svc.service.name + "', yet ports are ambiguous. Please use @WebService/Provider(portName=)");
        // there's only one
        return (TestEndpoint) svc.service.endpoints.toArray()[0];
    }
}
