/*
 * Copyright (c) 1997, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.container.javase;

import bsh.EvalError;
import com.sun.istack.NotNull;
import com.sun.xml.ws.test.World;
import com.sun.xml.ws.test.client.InterpreterEx;
import com.sun.xml.ws.test.container.AbstractApplicationContainer;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.CodeGenerator;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.container.WAR;
import com.sun.xml.ws.test.container.jelly.EndpointInfoBean;
import com.sun.xml.ws.test.model.TestEndpoint;
import com.sun.xml.ws.test.tool.WsTool;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * Container to deploy Java SE endpoints using java.xml.ws.Endpoint API
 *
 * @author Ken Hofsass
 * @author Jitendra Kotamraju
 */
public class JavaSeContainer extends AbstractApplicationContainer {

    //private final int port;
    private final Set<String> unsupportedUses;

    public JavaSeContainer(WsTool wsimport, WsTool wsgen, int port, Set<String> unsupportedUses) {
        super(wsimport, wsgen, false);
        //this.port = port;
        this.unsupportedUses = unsupportedUses;
    }

    @Override
    @NotNull
    public Set<String> getUnsupportedUses() {
        return unsupportedUses;
    }

    public String getTransport() {
        return "http";
    }

    public void start() throws Exception {
    }

    public void shutdown() {
    }

    @NotNull
    public Application deploy(DeployedService service) throws Exception {
        final String id = service.service.getGlobalUniqueName();
        final WAR war = assembleWar(service);
        List<EndpointInfoBean> beans = war.getEndpointsInfos();

        // Using a free port rather than a standard port since starting and
        // stopping multiple times has BindingException with light weight http server
        URL baseAddress = new URL("http://localhost:" + getFreePort() + "/" + id+"/");

        // serviceClassLoader.getResource("WEB-INF/wsdl/xxx.wsdl") should work,
        // so adjust the classpath accordingly
        final URLClassLoader serviceClassLoader = new URLClassLoader(new URL[]{service.warDir.toURL(), new File(service.warDir, "WEB-INF/classes").toURL()},
                                                                     World.runtime.getClassLoader());


        Object[] servers = new Object[service.service.endpoints.size()];
        int i = 0;
        for(TestEndpoint testEndpoint :service.service.endpoints) {

            final InterpreterEx interpreter = new InterpreterEx(serviceClassLoader);

            // Associate an EndpointInfoBean with the TestEndpoint
            EndpointInfoBean endpointInfoBean = null;
            for(Object bean : beans.toArray()) {
                EndpointInfoBean ebean = (EndpointInfoBean)bean;
                if (ebean.getImplementation().equals(testEndpoint.className)) {
                    endpointInfoBean = ebean;
                    break;
                }
            }

            if (service.service.isSTS) {
                updateWsitClient(war, service, baseAddress + testEndpoint.name);
            }

            final Class endpointClass = serviceClassLoader.loadClass(testEndpoint.className);

            final Object endpointImpl = endpointClass.newInstance();

            // Check if primary wsdl is specified via @WebService(wsdlLocation="")
            String wsdlLocation = null;
            Annotation[] anns = endpointClass.getAnnotations();
            for(Annotation ann : anns) {
                try {
                    Method method = ann.getClass().getDeclaredMethod("wsdlLocation");
                    String str = (String)method.invoke(ann);
                    if (!str.equals("")) {
                        wsdlLocation = str;
                        break;
                    }
                } catch(NoSuchMethodException e) {
                    // OK, the annotation does not support wsdlLocation() method
                }
            }

            // Collect all WSDL, Schema metadata for this service
            final List<Source> metadata = new ArrayList<Source>();
            collectDocs(service.warDir.getCanonicalPath()+"/", "WEB-INF/wsdl/", serviceClassLoader, metadata);
            
            // primary wsdl shouldn't be added, if it is already set via @WebService(wsdlLocation=)
            if (wsdlLocation != null) {
                Iterator<Source> it = metadata.iterator();
                while(it.hasNext()) {
                    Source source = it.next();
                    if (source.getSystemId().endsWith(wsdlLocation)) {
                        it.remove();
                    }
                }
            }
            System.out.print("Setting metadata="+metadata);

            // Set service name, port name
            Map<String, Object> props = new HashMap<String, Object>();
            if (endpointInfoBean != null && endpointInfoBean.getServiceName() != null) {
                // Endpoint.WSDL_SERVICE
                props.put("jakarta.xml.ws.wsdl.service", endpointInfoBean.getServiceName());
            }
            if (endpointInfoBean != null && endpointInfoBean.getPortName() != null) {
                // Endpoint.WSDL_PORT
                props.put("jakarta.xml.ws.wsdl.port", endpointInfoBean.getPortName());
            }
            System.out.println("Setting properties="+props);

            String endpointAddress = baseAddress+ testEndpoint.name;
            System.out.println("Endpoint Address="+endpointAddress);

            interpreter.set("endpointAddress", endpointAddress);
            interpreter.set("endpointImpl", endpointImpl);
            interpreter.set("metadata", metadata);
            interpreter.set("properties", props);

            //MetadataReader metadatareader =//
            Object feature = createMetadataFeature(service, interpreter);
            interpreter.set("feature", feature);

            CodeGenerator.generateDeploySources(war, testEndpoint, metadata, props, endpointAddress, wsdlLocation, !service.service.wsdl.isEmpty());

            try {
                String statements = "      jakarta.xml.ws.Endpoint endpoint = jakarta.xml.ws.Endpoint.create(endpointImpl" +
                        (feature != null ? ", new jakarta.xml.ws.WebServiceFeature[] {feature});" : ");\n") +
                        "      endpoint.setMetadata(metadata);\n" +
                        "      endpoint.setProperties(properties);\n" +
                        "      endpoint.publish(\"" + endpointAddress + "\");\n" +
                        "      return endpoint;\n";
                servers[i++] = interpreter.eval(statements);
            } catch(Throwable t) {
                t.printStackTrace();
                throw new Exception("Deploying endpoint "+ endpointAddress +" failed", t);
            }
        }
        return new JavaSeApplication(servers, baseAddress, service);
    }

    private Object createMetadataFeature(DeployedService service, InterpreterEx interpreter) throws EvalError {

        if (service.service.parent.metadatafiles == null || service.service.parent.metadatafiles.isEmpty()) {
            return null;
        }

        String script = "com.oracle.webservices.api.databinding.ExternalMetadataFeature.builder().addFiles( metadataFiles ).build()";
        File [] files = new File[service.service.parent.metadatafiles.size()];
        int i = 0;
        for(String path : service.service.parent.metadatafiles) {
            files[i++] = new File(service.service.baseDir + File.separator +  path);
        }
        interpreter.set("metadataFiles", files);
        return interpreter.eval(script);
    }

    private Set<String> getResourcePaths(String root, String path) {
        Set<String> r = new HashSet<String>();
        File[] files = new File(root+path).listFiles();
        if (files == null) {
            return null;
        }
        for( File f : files) {
            if(f.isDirectory()) {
                r.add(path+f.getName()+'/');
            } else {
                r.add(path+f.getName());
            }
        }
        return r;
    }

    private static int getFreePort() {

        // use static port in case you want to run it in plain java
        if (CodeGenerator.isGenerateTestSources()) {
            return CodeGenerator.getFreePort();
        }

        int port = -1;
        try {
            ServerSocket soc = new ServerSocket(0);
            port = soc.getLocalPort();
            soc.close();
        } catch (IOException e) {
        }
        return port;
    }


    /*
     * Get all the WSDL & schema documents recursively.
     */
    private void collectDocs(String root, String dirPath, ClassLoader loader, List<Source> metadata) throws IOException {
        Set<String> paths = getResourcePaths(root, dirPath);
        if (paths != null) {
            for (String path : paths) {
                if (path.endsWith("/")) {
                    if (path.endsWith("/CVS/") || path.endsWith("/.svn/")) {
                        continue;
                    }
                    collectDocs(root, path, loader, metadata);
                } else {
                    URL res = loader.getResource(path);
                    metadata.add(new StreamSource(res.openStream(), res.toExternalForm()));
                }
            }
        }
    }

}
