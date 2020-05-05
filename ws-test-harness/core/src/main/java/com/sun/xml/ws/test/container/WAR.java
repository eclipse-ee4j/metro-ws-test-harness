/*
 * Copyright (c) 1997, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.container;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.CodeGenerator;
import com.sun.xml.ws.test.Realm;
import com.sun.xml.ws.test.World;
import com.sun.xml.ws.test.container.jelly.EndpointInfoBean;
import com.sun.xml.ws.test.container.jelly.WebXmlInfoBean;
import com.sun.xml.ws.test.model.TestEndpoint;
import com.sun.xml.ws.test.model.WSDL;
import com.sun.xml.ws.test.tool.WsTool;
import com.sun.xml.ws.test.util.ArgumentListBuilder;
import com.sun.xml.ws.test.util.FileUtil;
import com.sun.xml.ws.test.util.FreeMarkerTemplate;
import com.sun.xml.ws.test.util.JavacTask;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.Path;

import jakarta.jws.WebService;
import javax.xml.namespace.QName;
import jakarta.xml.ws.WebEndpoint;
import jakarta.xml.ws.WebServiceClient;
import jakarta.xml.ws.WebServiceProvider;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

/**
 * Represents an exploded WAR file on a file system.
 *
 * This class primarily contains operations that generate various files that
 * constitute a war file.
 */
public final class WAR {
    /**
     * The root directory of the war image.
     */
    public final File root;

    /**
     * "build/classes" directory under the work directory. It is used
     * often enough that it is created here to avoid typo errors.
     */
    public final File classDir;

    /**
     * "WEB-INF" directory under the work directory. It is used
     * often enough that it is created here to avoid typo errors.
     */
    public final File webInfDir;

    /**
     * "WEB-INF/lib" directory.
     */
    public final File libDir;

    /**
     * Directory to put additional generated source files.
     */
    public final File srcDir;

    /**
     * One web application may end up having multiple WSDLs if a fromjava service
     * contains multiple @WebService classes.
     */
    private final List<File> wsdl = new ArrayList<File>();

    /**
     * This war file is created for this service.
     */
    public final DeployedService service;


    public WAR(DeployedService service) {
        this.service = service;
        root = service.warDir;
        webInfDir = new File(root,"WEB-INF");
        classDir = new File(webInfDir,"classes");
        classDir.mkdirs();
        libDir = new File(webInfDir,"lib");
        libDir.mkdir();
        srcDir = new File(service.workDir,"gen-src");
        srcDir.mkdir();
    }

    /**
     * This method collects info about endpoints; it's being used for generating
     * server side descriptors.
     *
     * @return
     * @throws Exception
     */
    public List<EndpointInfoBean> getEndpointsInfos() throws Exception {

        Set<TestEndpoint> endpoints = service.service.endpoints;
        ArrayList<EndpointInfoBean> beans = new ArrayList<EndpointInfoBean>();

        // fromJava:
        if (service.service.wsdl.isEmpty() || service.service.parent.metadatafiles.size() > 0) {

            for (TestEndpoint endpoint : endpoints) {
                EndpointInfoBean bean = EndpointInfoBean.create(
                        endpoint.name,
                        endpoint.className,
                        null,
                        null,
                        null,
                        null,
                        "/" + endpoint.name);
                beans.add(bean);
            }
        } else { // fromWSDL:

            ArrayList<String> portNames = new ArrayList<String>();

            // if we find multiple implClass, use the port local name to match them
            HashMap<String, String> portNameToImpl = new HashMap<String, String>();
            HashMap<String, String> implToPort = new HashMap<String, String>();
            // port name to service name
            HashMap<String, QName> portNameToServiceName = new HashMap<String, QName>();

            String implClass = null;

            ClassLoader loader = new URLClassLoader(
                    new URL[]{classDir.toURL()},
                    World.runtime.getClassLoader()
            );

            WebServiceClient wsca = null;
            for (String className : FileUtil.getClassFileNames(classDir)) {

                Class clazz = loader.loadClass(className);

                // prevent setting null ...
                if (clazz.getAnnotation(WebServiceClient.class) != null) {
                    wsca = (WebServiceClient) clazz.getAnnotation(WebServiceClient.class);
                    for (Method method : clazz.getMethods()) {
                        WebEndpoint a = method.getAnnotation(WebEndpoint.class);
                        if (a != null && method.getParameterTypes().length == 0) {
                            String name = a.name();
                            if (!name.equals("")) {
                                portNames.add(name);
                                portNameToServiceName.put(name, new QName(wsca.targetNamespace(), wsca.name()));
                            }
                        }
                    }
                    continue;
                }

                WebService ws = (WebService) clazz.getAnnotation(WebService.class);
                if (ws != null) {
                    String endpointInterface = ws.endpointInterface();
                    if (!endpointInterface.equals(""))
                        implClass = clazz.getName();
                    if (!"".equals(ws.portName())) {
                        portNameToImpl.put(ws.portName(), implClass);
                        implToPort.put(implClass, ws.portName());
                    }
                } else {
                    WebServiceProvider wsp = (WebServiceProvider) clazz.getAnnotation(WebServiceProvider.class);
                    if (wsp != null) {
                        implClass = clazz.getName();
                        if (!"".equals(wsp.portName())) {
                            portNameToImpl.put(wsp.portName(), implClass);
                            implToPort.put(implClass, wsp.portName());
                        }
                    }
                }
            }

            // create endpoint info beans
            String tns = null;
            String wsdlLocation = null;

            if (wsca != null) {
                tns = wsca.targetNamespace();

                // hacky
                wsdlLocation = wsca.wsdlLocation();
                wsdlLocation = wsdlLocation.replace('\\', '/');
                wsdlLocation = "WEB-INF/wsdl/" +
                        wsdlLocation.substring(
                                wsdlLocation.lastIndexOf("/") + 1,
                                wsdlLocation.length());
            }

            int i = 0;
            for (String portName : portNames) {
                String impl = portNameToImpl.get(portName);
                portNameToImpl.remove(portName);
                if (impl == null) {
                    if (portNames.size() > 1) {
                        continue;   // OK to have a portname without a deployed endpoint
                    }
                    impl = implClass; // default
                }

                // call EndpointInfoBean.create thanks to static import.
                // this allows us to create an instance of class that's not visible to this classloader.
                EndpointInfoBean bean = EndpointInfoBean.create(
                        "endpoint" + (i++),
                        impl,
                        wsdlLocation,
                        portNameToServiceName.get(portName),
                        new QName(tns, portName),
                        "binding",
                        "/" + service.service.getEndpointByImpl(impl).name);
                beans.add(bean);
                implToPort.remove(impl);
            }

            for (Entry<String, String> e : implToPort.entrySet()) {
                EndpointInfoBean bean = EndpointInfoBean.create(
                        "endpoint" + (i++),
                        e.getKey(),
                        wsdlLocation,
                        portNameToServiceName.get(e.getValue()),
                        new QName(tns, e.getValue()),
                        "binding",
                        "/" + service.service.getEndpointByImpl(e.getKey()).name);
                beans.add(bean);
            }
            // error check
            if (!portNameToImpl.isEmpty())
                throw new Exception("Implementations " + new ArrayList(portNameToImpl.values()) + " don't have corresponding ports in WSDL." +
                        " Their declared ports are " + new ArrayList(portNameToImpl.keySet()) +
                        " but actual ports are " + portNames
                );
        }
        return beans;
    }

    /**
     * Creates a war archive from the exploded image at {@link #root}.
     */
    public void zipTo(File archive) throws Exception {
        Zip zip = new Zip();
        zip.setProject(World.project);
        zip.setDestFile(archive);
        zip.setBasedir(root);
        zip.execute();
    }

    /**
     * Copies the classpath specified by the given {@link Path}
     * into <code>WEB-INF/lib</code> and <code>WEB-INF/classes</code>
     */
    public void copyClasspath(Realm classpath) throws Exception {
        int n = 0;
        for (File path : classpath.list()) {
            if(path.isFile())
                // just copy one jar
                FileUtil.copyFile(path,new File(libDir,path.getName()));
            else {
                // create an uncompressed jar file. This serves a few purposes.
                //  - in general file systems are not good at dealing with many small files
                //  - we'll do the archiving anyway when we pack this into a jar
                Jar jar = new Jar();
                jar.setProject(World.project);
                jar.setDestFile(new File(libDir,"generated"+(n++)+".jar"));
                jar.setBasedir(path);
                jar.setCompress(false);
                jar.execute();
            }
        }
    }

    /**
     * Copies handler files in to WEB-INF/classes
     */
    public void copyToClasses(File ... handlerConfigs) {
        for (File config : handlerConfigs) {
            FileUtil.copyFile(config, new File(classDir, config.getName()));
        }
    }

    /**
     * Copies web.xml to WEB-INF/
     */
    public void copyToWEBINF(File file) {
        FileUtil.copyFile(file, new File(webInfDir, file.getName()));
    }

    /**
     * Copies resources under the directory in to <code>WEB-INF/classes</code>
     */
    public void copyResources(File resourcesDir) {
        if(resourcesDir != null) {
            FileUtil.copyDir(resourcesDir, classDir, "**/wsit-client.xml");
        }
    }

    public void copyWsit(File wsitConf) {
        if(wsitConf != null && wsitConf.exists()) {
            FileUtil.copyFile(wsitConf, new File(classDir, wsitConf.getName()));
        }
    }

    /**
     * Gets the path of the WSDL.
     *
     * <p>
     * This is either copied from the test data (for "fromwsdl" tests),
     * or generated (for "fromjava" tests.) For fromjava tests with
     * multiple <code>WebService</code> classes, you may get more than one WSDLs.
     *
     * <p>
     * In a situation where there's no WSDL and just provider service,
     * the list may be empty.
     */
    public @NotNull List<File> getWSDL() {
        return wsdl;
    }

    /**
     * This method uses Jelly to write the sun-jaxws.xml file. The
     * template file is sun-jaxws.jelly.
     *
     * @return
     *      list of endpoints that were discovered.
     */
    final void generateSunJaxWsXml(List<EndpointInfoBean> endpointInfoBeans) throws Exception {
        FreeMarkerTemplate jelly = new FreeMarkerTemplate("/web/sun-jaxws.ftl");
        jelly.put("endpointInfoBeans", endpointInfoBeans);
        jelly.run(new File(webInfDir, "sun-jaxws.xml"));
    }

    /**
     * This method uses Jelly to write the web.xml file. The
     * template file is web.jelly. The real work happens
     * in the WebXmlInfoBean object which supplies information
     * to the Jelly processor through accessor methods.
     *
     * @see WebXmlInfoBean
     */
    final void generateWebXml(List<EndpointInfoBean> endpoints, boolean httpspi) throws Exception {
        FreeMarkerTemplate jelly = new FreeMarkerTemplate("/web/web.ftl");
        String listenerClass = httpspi
                ? "com.sun.xml.ws.transport.httpspi.servlet.WSSPIContextListener"
                : "com.sun.xml.ws.transport.http.servlet.WSServletContextListener"  ;
        String servletClass = httpspi
                ? "com.sun.xml.ws.transport.httpspi.servlet.WSSPIServlet"
                : "com.sun.xml.ws.transport.http.servlet.WSServlet";
        WebXmlInfoBean infoBean = new WebXmlInfoBean(service.parent,endpoints, listenerClass, servletClass );
        jelly.put("data", infoBean);
        jelly.run(new File(webInfDir, "web.xml"));
    }

    /**
     * Generates <code>sun-web.xml</code>
     */
    final void generateSunWebXml() throws Exception {
        FreeMarkerTemplate jelly = new FreeMarkerTemplate("/web/sun-web.ftl");
        jelly.run(new File(webInfDir, "sun-web.xml"));
    }

    /**
     * Generate server artifacts from WSDL.
     */
    final void compileWSDL(WsTool wsimport) throws Exception {
        assert !service.service.wsdl.isEmpty();
        assert this.wsdl.isEmpty();

        for (WSDL dl : service.service.wsdl) {
            ArgumentListBuilder options = new ArgumentListBuilder();
            //Add customization files
            for (File custFile : service.service.customizations) {
                options.add("-b").add(custFile);
            }
            options.add("-extension");
            //Don't add the default package option if -noPackage is specified
            // this will be helpful in testing default/customization behavior.
            if (!service.service.parent.testOptions.contains("-noPackage")) {

                // set package name if not specified in wsimport-server options
                if (!service.service.parent.wsimportServerOptions.contains("-p")) {
                    options.add("-p").add(service.service.getGlobalUniqueName());
                }
            }
            //Other options
            if (World.debug) {
                options.add("-verbose");
            }
            options.add("-s").add(srcDir);
            options.add("-d").add(classDir);
            options.add("-Xnocompile");
            if (Boolean.getBoolean("harness.useSSL")) {
                options.add("-XdisableSSLHostnameVerification");
            }
            options.add(dl.wsdlFile);
            options.addAll(service.service.parent.wsimportServerOptions);
            if (!wsimport.isNoop()) {
                System.out.println("Generating server artifacts from " + dl.wsdlFile);
                options.invoke(wsimport);
            }

            // copy WSDL into a war file
            File wsdlDir = new File(webInfDir, "wsdl");
            if (dl.relativeLocation != null) {
                wsdlDir = new File(wsdlDir, dl.relativeLocation);
            }
            wsdlDir.mkdirs();

            File src = dl.wsdlFile;
            File wsdlFile = new File(wsdlDir, src.getName());
            this.wsdl.add(wsdlFile);

            FileUtil.copyFile(src, wsdlFile);
            for (File importedWsdl : dl.importedWsdls) {
                String importedPath = importedWsdl.getCanonicalPath().substring(src.getParentFile().getCanonicalPath().length() + 1);
                FileUtil.copyFile(importedWsdl, new File(wsdlDir, importedPath));
            }
            for (File schema : dl.schemas) {
                String importedPath = schema.getCanonicalPath().substring(src.getParentFile().getCanonicalPath().length() + 1);
                FileUtil.copyFile(schema, new File(wsdlDir, importedPath));
            }
        }
    }

    /**
     * Compiles Java source files into <code>WEB-INF/classes</code>.
     */
    final void compileJavac() throws Exception {
        JavacTask javac = new JavacTask(service.parent.descriptor.javacOptions);
        if(service.parent.descriptor.common != null)
            javac.setSourceDir(service.service.baseDir, srcDir,
                    service.parent.descriptor.common );
        else
            javac.setSourceDir(service.service.baseDir, srcDir);

        javac.setDestdir(classDir);
        javac.setDebug(true);
        CodeGenerator.generateJavac(javac);
        javac.execute();
    }

    /**
     * Generates a WSDL into a war file if this is "fromjava" service.
     */
    final void generateWSDL(WsTool wsgen) throws Exception {
        assert service.service.wsdl.isEmpty();

        // Use wsgen to generate the artifacts
        File wsdlDir = new File(webInfDir, "wsdl");
        wsdlDir.mkdirs();

        for (TestEndpoint endpt : service.service.endpoints) {
            if(endpt.isProvider)    continue;
            ArgumentListBuilder options = new ArgumentListBuilder();
            options.add("-wsdl");
            if(World.debug)
                options.add("-verbose");
            options.add("-r").add(wsdlDir);
            Path cp = new Path(World.project);
            cp.createPathElement().setLocation(classDir);
            cp.add(World.tool.getPath());
            cp.add(World.runtime.getPath());
            if(World.debug)
                System.out.println("wsgen classpath arg = " + cp);
            options.add("-cp").add(cp);
            options.add("-s").add(service.service.parent.disgardWsGenOutput ? NOWHERE : classDir);
            options.add("-d").add(service.service.parent.disgardWsGenOutput ? NOWHERE : classDir);

            // obtain a report file from wsgen
            File report = new File(wsdlDir,"wsgen.report");
            options.add("-XwsgenReport").add(report);

            // additional wsgen options from test descriptor
            options.addAll(service.service.parent.wsgenOptions);

            options.add(endpt.className);

            System.out.println("Generating WSDL");
            if(World.debug)
                System.out.println(options);
            options.invoke(wsgen);

            // parse report
            Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(report);
            wsdl.add(new File(dom.getDocumentElement().getElementsByTagName("wsdl").item(0).getTextContent().trim()));
        }
    }

    private static final File NOWHERE = new File(System.getProperty("java.io.tmpdir"));
}
