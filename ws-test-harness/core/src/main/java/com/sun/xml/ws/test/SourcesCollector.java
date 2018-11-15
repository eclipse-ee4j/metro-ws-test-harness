/*
 * Copyright (c) 2015, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper class to copy all necessary test artifacts out of harness (testcases directory)
 * to separate directory
 */
public class SourcesCollector {

    private static final Set IGNORED = new HashSet<String>();

    static {
        IGNORED.add(".DS_Store");
    }

    String source;

    public SourcesCollector(String source) {
        this.source = source;
    }

    public static void ensureDirectoryExists(String newDir) {
        File file = new File(newDir);
        if (!file.exists()) {
            ensureDirectoryExists(file.getParent());
            file.mkdir();
        }
    }

    public void copyFilesTo(String destDir) {
        List<String> files = collectFiles();

        for (String file : files) {
            File src = new File(source + "/" + file);
            File dst = new File(destDir + "/" + file);
            ensureDirectoryExists(dst.getParent());
            try {
                copy(src, dst);
            } catch (IOException e) {
                System.err.println("Error copying file: [" + src + "] to [" + dst + "]");
                e.printStackTrace();
            }
        }
    }

    public static void copy(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            System.out.println("copy [" + source.getAbsoluteFile() + "]\n" +
                               "  to [" + dest.getAbsoluteFile() + "].");
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length, totalLength = 0;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
                totalLength += length;
            }
            System.out.println("totalLength = " + totalLength);
        } finally {
            if (is != null) is.close();
            if (os != null) os.close();
        }

    }

    protected List<String> collectFiles() {
        File f = new File(source);
        File[] files = f.listFiles();
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            collectFile(file, result);
        }
        return result;
    }

    private void collectFile(File file, List<String> files) {
        if (file.isDirectory()) {
            for(File f : file.listFiles()) {
                collectFile(f, files);
            }
        } else {
            String result = file.getAbsoluteFile().toString().replaceAll(source, "");
            if (result.startsWith("/")) result = result.substring(1);
            if (!IGNORED.contains(result)) {
                System.out.println("file: [" + result + "]");
                files.add(result);
            }
        }
    }

}
