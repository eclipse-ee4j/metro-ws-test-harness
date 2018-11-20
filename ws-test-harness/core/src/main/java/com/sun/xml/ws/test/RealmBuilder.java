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

import org.apache.tools.ant.types.Path;
import org.codehaus.classworlds.ClassRealm;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Adds jar files to {@link ClassRealm} and also builds up {@link Path}
 * to remember what those jars are.
 *
 * @author Kohsuke Kawaguchi
 */
public final class RealmBuilder {
    private final ClassRealm realm;
    private Path classPath;

    public RealmBuilder(ClassRealm realm, Path classPath) {
        this.realm = realm;
        this.classPath = classPath;
    }

    public RealmBuilder(ClassRealm realm) {
        this(realm,new Path(World.project));
    }

    /**
     * Adds a single jar.
     */
    public void addJar(File jar) throws IOException {
        if(!jar.exists())
            throw new IOException("No such file: "+jar);
        realm.addConstituent(jar.toURL());

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
}
