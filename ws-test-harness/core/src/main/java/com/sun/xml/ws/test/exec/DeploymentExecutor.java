/*
 * Copyright (c) 1997, 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.exec;

import com.sun.xml.ws.test.World;
import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.CodeGenerator;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.model.TestEndpoint;
import com.sun.xml.ws.test.model.TestService;
import com.sun.xml.ws.test.util.ArgumentListBuilder;
import com.sun.xml.ws.test.util.JavacTask;
import com.sun.xml.ws.test.util.WSITUtil;
import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@link TestCase} that deploys a {@link TestService} to
 * the {@link ApplicationContainer}.
 *
 * <p>
 * After this test is run, {@link #createUndeployer()} needs
 * to be used to undeploy the deployed service.
 *
 * @author Kohsuke Kawaguchi
 */
public class DeploymentExecutor extends Executor {
    private final DeployedService context;

    public DeploymentExecutor(DeployedService context) {
        super("Deploy "+context.service.name,context.parent);
        this.context = context;
    }

    public void runTest() throws Throwable {
        CodeGenerator.testStarting(context.workDir);
        // deploy the service
        context.app = context.parent.container.deploy(context);
        //For STS we do not want to generate client artifacts
        if (!context.service.isSTS) {
            // then use that WSDL to generate client
            generateClientArtifacts();
        } else {
            addSTSToClasspath();
            updateWsitClient();
        }
    }

    public void updateWsitClient()throws Exception {
        File wsitClientFile = new File(context.parent.getResources(),"wsit-client.xml");
        if (wsitClientFile.exists()) {
            URI uri = context.app.getEndpointAddress((TestEndpoint)context.service.endpoints.toArray()[0]);
            WSITUtil.updateWsitClient(wsitClientFile, uri.toString(), context.service.wsdl.get(0).wsdlFile.toURI().toString());
        } else {
            throw new RuntimeException("wsit-client.xml is absent. It is required. \n"+
                    "Please check " + context.service.parent.resources );
        }
    }

    public void addSTSToClasspath() throws Exception{
        List<URL> classpath = context.clientClasspaths;

        ClassLoader baseCl = World.runtime.getClassLoader();
        if (context.parent.clientClassLoader != null) {
            baseCl = context.parent.clientClassLoader;
        }

        classpath.add(new File(context.warDir, "WEB-INF/classes").toURL());

        context.parent.clientClassLoader= new URLClassLoader( classpath.toArray(new URL[classpath.size()]), baseCl );

    }
    /**
     * Generate & compile source files from service WSDL.
     */
    private void generateClientArtifacts() throws Exception {

        File gensrcDir = makeWorkDir("client-source");
        File classDir = makeWorkDir("client-classes");

        for (int i = 0; i < context.app.getWSDL().size(); i++) {
            URL wsdl = context.app.getWSDL().get(i);
            ArgumentListBuilder options = new ArgumentListBuilder();
            // Generate cusomization file & add as wsimport option

            // we used to do this just to set the package name, but
            // it turns out we can do it much easily with the -p option
            //options.add("-b");
            //options.add(genClientCustomizationFile(context).getAbsolutePath());

            //Don't add the default package option if -noPackage is specified
            // this will be helpful in testing default/customization behavior.
            if (!context.service.parent.testOptions.contains("-noPackage")) {

                // set package name. use 'client' to avoid collision between server artifacts
                if (!context.service.parent.wsimportClientOptions.contains("-p")) {
                    if (i > 0) {
                        options.add("-p").add(context.parent.descriptor.name + ".client" + (i + 1));
                    } else {
                        options.add("-p").add(context.parent.descriptor.name + ".client");
                    }
                }
            }
            options.add("-extension");

            //Add user's additional customization files
            for (File custFile : context.parent.descriptor.clientCustomizations)
                options.add("-b").add(custFile);

            //Other options
            if(World.debug)
                options.add("-verbose");
            options.add("-s").add(gensrcDir);
            options.add("-d").add(classDir);
            options.add("-Xnocompile");
            if (Boolean.getBoolean("harness.useSSL")) {
                options.add("-XdisableSSLHostnameVerification");
            }
            options.add(wsdl);
            if(World.debug)
                System.out.println("wsdl = " + wsdl);
            options.addAll(context.service.parent.wsimportClientOptions);
            // compile WSDL to generate client-side artifact
            options.invoke(context.parent.wsimport);
        }

        // compile the generated source files to javac
        JavacTask javac = new JavacTask(context.parent.descriptor.javacOptions);
        javac.setSourceDir(
            gensrcDir,
            context.parent.descriptor.common,
            new File(context.parent.descriptor.home,"client")
        );
        javac.setDestdir(classDir);
        javac.setDebug(true);
        if(!context.parent.wsimport.isNoop())
            // if we are just reusing the existing artifacts, no need to recompile.
            javac.execute();

        // load the generated classes
        List<URL> classpath = context.clientClasspaths;
        classpath.add(classDir.toURL());
        // TODO: only the local container needs server classes in the classloader.
        classpath.add(new File(context.warDir, "WEB-INF/classes").toURL());
        if(context.getResources()!=null) {
            classpath.add(context.getResources().toURL());
        }

        /*
         * If there is a service like STS it has already been added to context.parent.clientClassLoader
         *  add that to the final classpath
         */
        if (context.parent.clientClassLoader instanceof URLClassLoader) {
            URL [] urls = ((URLClassLoader)context.parent.clientClassLoader).getURLs();
            classpath.addAll(Arrays.asList(urls));
        }

        ClassLoader cl = new URLClassLoader( classpath.toArray(new URL[classpath.size()]),
                World.runtime.getClassLoader() );

        context.parent.clientClassLoader = cl;

        // The following code scans the generated source files and look for the class
        // that extend from Service. This could be as simple as
        // line-by-line scan of "extends Service" ---
        // if we want to be more robust, we can write an AnnotationProcessor
        // so that we can work on top of Java AST, but this simple grep-like
        // approach would work just fine with wsimport.

        List<String> serviceClazzNames = new ArrayList<String>();
        findServiceClass(gensrcDir,serviceClazzNames);

        if (serviceClazzNames.isEmpty())
            System.out.println("WARNING: Cannot find the generated 'service' class that extends from jakarta.xml.ws.Service. Assuming provider-only service");

        for (String name : serviceClazzNames)
            context.serviceClass.add(cl.loadClass(name));
    }

    /**
     * Creates another test to be exeucted at the end
     * to undeploy the service that this test deployed.
     */
    public Executor createUndeployer() {
        return new Executor("Undeploy "+context.service.name,context.parent) {
            public void runTest() throws Throwable {
                CodeGenerator.testStarting(context.workDir);
                if(DeploymentExecutor.this.context.app!=null)
                    DeploymentExecutor.this.context.app.undeploy();
            }
        };
    }

    /**
     * Recursively scans the Java source directory and find a class
     * that extends from "Service", add them to the given list.
     */
    private void findServiceClass(File dir,List<String> result) throws Exception {
        OUTER:
        for (File child : dir.listFiles()) {
            if (child.isDirectory()) {
                findServiceClass(child,result);
            } else
            if (child.getName().endsWith(".java")) {
                // check if this is the class that extends from "Service"

                BufferedReader reader = new BufferedReader(new FileReader(child));
                String pkg = null;  // this variable becomes Java package of this source file
                String line;
                while ((line =reader.readLine()) != null){
                    if(line.startsWith("package ")) {
                        pkg = line.substring(8,line.indexOf(';'));
                    }
                    if (line.contains("extends Service")){
                        // found it.
                        reader.close();

                        String className = child.getName();
                        // remove .java from the fileName
                        className = className.substring(0,className.lastIndexOf('.'));
                        //Get the package name for the file by taking a substring after
                        // client-classes and replacing '/' by '.'
                        result.add(pkg+'.'+ className);

                        continue OUTER;
                    }
                }
                reader.close();
            }
        }
    }

    /*
     * Generates a JAX-WS customization file for generating client artifacts.
     */
    //private File genClientCustomizationFile(DeployedService service) throws Exception {
    //    File customizationFile = new File(service.workDir, "custom-client.xml");
    //    OutputStream outputStream =
    //        new FileOutputStream(customizationFile);
    //    XMLOutput output = XMLOutput.createXMLOutput(outputStream);
    //
    //    String packageName = service.service.parent.shortName;
    //
    //    // to avoid collision between the client artifacts and server artifacts
    //    // when testing everything inside a single classloader (AKA local transport),
    //    // put the client artifacts into a different package.
    //    CustomizationBean infoBean = new CustomizationBean(packageName+".client",
    //                                        service.app.getWSDL().toExternalForm());
    //    JellyContext jellyContext = new JellyContext();
    //    jellyContext.setVariable("data", infoBean);
    //    jellyContext.runScript(getClass().getResource("custom-client.jelly"),output);
    //    output.flush();
    //
    //    return customizationFile;
    //}
}

