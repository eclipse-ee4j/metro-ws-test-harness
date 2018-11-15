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

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * A service to be deployed for a test.
 *
 * <p>
 * TODO: We need to be able to mark service as an EJB service
 *
 * @author Kohsuke Kawaguchi
 */
public class TestService {

    /**
     * Name of the service.
     *
     * The name must be:
     * <ol>
     *  <li>Unique within {@link TestDescriptor}
     *  <li>an empty string or a valid Java identifier.
     * </ol>
     *
     * <p>
     * An empty string is convenient to describe the default/primary service
     * (or when there's just one service involved, which is the majority.)
     */
    @NotNull
    public final String name;

    /**
     * Directory in which the service's source files reside.
     */
    @NotNull
    public final File baseDir;

    /**
     * Optional WSDL file that describes this service.
     */
    @Nullable
    public final List<WSDL> wsdl;

    /**
     * Possibly empty list of JAXB/JAX-WS external binding customizations.
     *
     * Must be empty when {@link #wsdl} is null.
     */
    @NotNull
    public final List<File> customizations = new ArrayList<File>();

    /**
     * {@link TestEndpoint}s that this service exposes.
     *
     * <p>
     * The harness uses this information to inject proxies to the client.
     */
    @NotNull
    public final Set<TestEndpoint> endpoints = new LinkedHashSet<TestEndpoint>();

    public final TestDescriptor parent;

    /**
     * Determines if the service is an STS for WSTrust and needs special handling
     */
    public boolean isSTS;

    /**
     * @param explicitServiceClassName
     *      Descriptor can explicitly specify the service class name.
     *      If this happens, we won't search for @WebService classes and just use this instead.
     *      Used for deploying inner classes and testing inheritance.
     */
    public TestService(TestDescriptor parent, String name, File baseDir, List<WSDL> wsdl, boolean sts, @Nullable String explicitServiceClassName) throws IOException {
        this.parent = parent;
        this.name = name;
        this.wsdl = wsdl;
        this.baseDir = baseDir;
        this.isSTS = sts;

        if(explicitServiceClassName==null) {
            // search for classes with @WebService
            findEndpoints(baseDir);
        } else {
            String shortName = explicitServiceClassName.substring(explicitServiceClassName.lastIndexOf('.')+1);
            endpoints.add(new TestEndpoint(shortName,explicitServiceClassName,null,false));
        }
    }

    public String getAbsolutePath(String relativePath) {
        return baseDir.getAbsolutePath() + File.separator + relativePath;
    }

    /**
     * Gets the {@link TestEndpoint} that has the specified implementation class.
     */
    public TestEndpoint getEndpointByImpl(String implClassFullName) {
        for (TestEndpoint ep : endpoints) {
            if(ep.className.equals(implClassFullName))
                return ep;
        }
        throw new Error("No TestEndpoint object recorded for "+implClassFullName);
    }

    /**
     * Returns the name combined with the test case name to make a globaly unique name
     * for this service.
     */
    public String getGlobalUniqueName() {
        if(name.length()==0)
            return parent.name;
        else
            return parent.name +'.'+name;
    }

    /**
     * Scans the Java source code in the server directory and
     * find all classes with @WebService. Those are turned into
     * {@link TestEndpoint}. 
     */
    private void findEndpoints(File dir) throws IOException {
        File[] dirs = dir.listFiles(new FileFilter() {
            public boolean accept(File child) {
                // don't go in our own work directory
                return child.isDirectory() && !child.getName().equals("work");
            }
        });
        Arrays.sort(dirs, new Comparator<File>() {

            public int compare(File f1, File f2) {
                return f2.compareTo(f1);
            }
        });
        for (File subdir : dirs) {
            findEndpoints(subdir);
        }

        File[] javas = dir.listFiles(new FileFilter() {
            public boolean accept(File child) {
                return child.getName().endsWith(".java");
            }
        });

        if (javas.length > 0) {
            // parse the Java file, looking for @WebService/Provider
            // (note that at this point those source files by themselves won't compile)
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

            // TODO: workaround for Jake/Jigsaw. Remove after fixing https://bugs.openjdk.java.net/browse/JDK-6929461
            if (compiler == null) {
                try {
                    Class compilerClass = Class.forName("com.sun.tools.javac.api.JavacTool");
                    Constructor constructor = compilerClass.getConstructor();
                    compiler = (JavaCompiler) constructor.newInstance();
                } catch (Throwable ignored) {
                    ignored.printStackTrace();
                }
            }
            // </TODO>

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager,
                    diagnostics, new ArrayList<String>() {{add("-proc:only");}}, null,
                    fileManager.getJavaFileObjects(javas));
            EndpointReader r = new EndpointReader();
            task.setProcessors(Collections.singleton(r));
            task.call();
            endpoints.addAll(r.getTestEndpoints());
        }
    }

    @Override
    public String toString() {
        return name+" of "+parent.toString();
    }

    /**
     * This filter gives all handler configuration files in the directory.
     * i.e files matching pattern *handlers.xml
     */
    class HandlersFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return (name.endsWith("handlers.xml"));
        }
    }

    public File[] getHandlerConfiguration() {
        return baseDir.listFiles(new HandlersFilter());
    }

    /**
     * This filter gives all web.xml files in the directory.
     * i.e files with name web.xml
     */
    class NameFilter implements FilenameFilter {
        String filename;
        NameFilter(String filename) {
            this.filename = filename;
        }
        public boolean accept(File dir, String name) {
            return (name.equals(filename));
        }
    }

    public File getConfiguredFile(String filename) {
        return first(baseDir.listFiles(new NameFilter(filename)));
    }

    private File first(File[] files) {
        return files == null || files.length == 0 ? null: files[0];
    }
}
