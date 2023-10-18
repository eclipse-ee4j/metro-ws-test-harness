/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.util;

import com.sun.xml.ws.test.World;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Class for utility methods for finding files and
 * information about them.
 */
public class FileUtil {

    public static final FileFilter DIRECTORY_FILTER = new FileFilter() {
        public boolean accept(File path) {
            return path.isDirectory();
        }
    };

    public static final FileFilter JAR_FILE_FILTER = new FileFilter() {
        public boolean accept(File path) {
            return path.getName().endsWith(".jar");
        }
    };

    /**
     * This method returns the fully qualified names of
     * class files below the given directory.
     */
    public static String [] getClassFileNames(File dir) {
        List<String> names = new ArrayList<String>();
        Stack<String> pathStack = new Stack<String>();
        addClassNames(dir, pathStack, names);
        return names.toArray(new String [names.size()]);
    }

    /**
     * Recursive method for adding class names under
     * a given top directory.
     */
    private static void addClassNames(File current, Stack<String> stack,
                                      List<String> names) {

        File[] children = current.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                stack.push(child.getName());
                addClassNames(child, stack, names);
                stack.pop();
            } else {
                if (child.getName().endsWith(".class")) {
                    names.add(createFullName(stack, child));
                }
            }
        }
    }

    /*
    * Create a fully-qualified path name.
    */
    private static String createFullName(Stack<String> dirs, File classFile) {
        String className = classFile.getName().substring(
            0, classFile.getName().indexOf(".class"));
        if (dirs.empty()) {
            return className;
        }
        StringBuilder fullName = new StringBuilder();
        for (String dir : dirs) {
            fullName.append(dir);
            fullName.append(".");
        }
        fullName.append(className);
        return fullName.toString();
    }

    /**
     * Recursively delete a directory and all its descendants.
     */
    public static void deleteRecursive(File dir) {
        Delete d = new Delete();
        d.setProject(World.project);
        d.setDir(dir);
        d.execute();
    }

    /**
     * Copies a single file.
     */
    public static void copyFile(File src, File dest) {
        Copy cp = new Copy();
        cp.setOverwrite(true);
        cp.setProject(World.project);
        cp.setFile(src);
        cp.setTofile(dest);
        cp.execute();
    }

    /**
     * Copies a whole directory recursively.
     */
    public static void copyDir(File src, File dest, String excludes) {
        Copy cp = new Copy();
        cp.setProject(World.project);
        cp.setTodir(dest);
        FileSet fs = new FileSet();
        if (excludes != null) {
            fs.setExcludes(excludes);
        }
        fs.setDir(src);
        cp.addFileset(fs);
        cp.execute();
    }

    public static File createTmpDir( boolean scheduleDeleteOnVmExit ) throws IOException {
        // create a temporary directory
        File dir = new File(".");
        File targetFolder = new File(dir, "target");
        if (targetFolder.exists()) {
            dir = targetFolder;
        }
        File tmpFile = Files.createTempDirectory(dir.toPath(), "wstest" + "tmp").toFile();
        if(scheduleDeleteOnVmExit) {
            tmpFile.deleteOnExit();
        }
        return tmpFile;
    }
}
