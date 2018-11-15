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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Bean shell script.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Script {
    private Script() {}

    /**
     * Name of the script.
     *
     * This is intended to allow humans to find out which script this is.
     */
    public abstract String getName();

    /**
     * Returns a new reader that reads the script.
     */
    public abstract Reader read() throws IOException;


    public String getSource() {
        Reader reader = null;
        try {
            reader = read();
            StringWriter writer = new StringWriter();
            char [] buf = new char[1024];
            int read;
            while ((read = reader.read(buf)) != -1) {
                writer.write(buf, 0, read);
            }
            return writer.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "<ERROR READING SCRIPT>";
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * {@link Script} where the script is given as literal text.
     */
    public static final class Inline extends Script {
        private final String name;
        private final String script;

        public Inline(String name, String script) {
            this.name = name;
            this.script = script;
        }

        /**
         * Use a portion of the script as the name.
         */
        public String getName() {
            return name;
        }

        public Reader read() {
            return new StringReader(script);
        }
    }

    /**
     * {@link Script} where the script is stored in a file.
     */
    public static final class File extends Script {
        private final java.io.File script;

        public File(java.io.File script) {
            this.script = script;
        }

        public String getName() {
            return script.getName();    // just use the file name portion
        }

        public Reader read() throws IOException {
            return new InputStreamReader(new FileInputStream(script),"UTF-8");
        }
    }
}
