/*
 * Copyright (c) 1997, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test;

import com.sun.istack.Nullable;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.types.Path;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Represents a classloader.
 *
 * {@link Realm}s form a tree structure where children delegates
 * to the parent for classloading.
 *
 * @author Kohsuke Kawaguchi
 */
public class Realm {
    /**
     * Human readable name that identifies this realm for the debugging purpose.
     */
    private final String name;

    /**
     * Parent realm. Class loading delegates to this parent.
     */
    private final @Nullable Realm parent;

    /**
     * Jar files and class folders that are added.
     */
    private final Path classPath = new Path(World.project);

    private AntClassLoader classLoader;

    public Realm(String name, Realm parent) {
        this.name = name;
        this.parent = parent;
    }

    public synchronized ClassLoader getClassLoader() {
        if(classLoader==null) {
            // delegates to the system classloader by default.
            // when invoked for debugging harness (with a lot of jars in system classloader),
            // this provides the easy debug path.
            // when invoked through bootstrap, this still provides the maximum isolation.
            ClassLoader pcl = ClassLoader.getSystemClassLoader();
            if(parent!=null)
                pcl = parent.getClassLoader();
            classLoader = new AntClassLoader();
            classLoader.setParent(pcl);
            classLoader.setProject(World.project);
            classLoader.setClassPath(classPath);
            classLoader.setDefaultAssertionStatus(true);
        }

        return classLoader;
    }

    /**
     * Adds a single jar.
     */
    public void addJar(File jar) throws IOException {
        assert classLoader==null : "classLoader is already created";
        if(!jar.exists())
            throw new IOException("No such file: "+jar);
        classPath.createPathElement().setLocation(jar);
    }

    /**
     * Adds a single class folder.
     */
    public void addClassFolder(File classFolder) throws IOException {
        addJar(classFolder);
    }

    /**
     * Adds all jars in the given folder.
     *
     * @param folder
     *      A directory that contains a bunch of jar files.
     * @param excludes
     *      List of jars to be excluded
     */
    public void addJarFolder(File folder, final String... excludes) throws IOException {
        if(!folder.isDirectory())
            throw new IOException("Not a directory "+folder);

        File[] children = folder.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                for (String name : excludes) {
                    if(pathname.getName().equals(name))
                        return false;   // excluded
                }
                return pathname.getPath().endsWith(".jar");
            }
        });

        for (File child : children) {
            addJar(child);
        }
    }

    public void dump(PrintStream out) {
        for( String item : classPath.toString().split(File.pathSeparator)) {
            out.println("  "+item);
        }
    }

    public String toString() {
        return name+" realm";
    }

    /**
     * List all the components in this realm (excluding those defined in parent.)
     */
    public File[] list() {
        String[] names = classPath.list();
        File[] r = new File[names.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = new File(names[i]);
        }
        return r;
    }

    public Path getPath() {
        return classPath;
    }

    public Class loadClass(String className) throws ClassNotFoundException {
        return getClassLoader().loadClass(className);
    }
}
