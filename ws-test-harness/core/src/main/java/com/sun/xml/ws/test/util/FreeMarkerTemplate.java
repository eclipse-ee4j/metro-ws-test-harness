/*
 * Copyright (c) 2015, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.util;


import com.sun.xml.ws.test.SourcesCollector;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FreeMarkerTemplate {

    Map root = new HashMap();
    String templateName;

    static Configuration cfg = new Configuration(Configuration.VERSION_2_3_21);

    static {
        cfg.setClassForTemplateLoading(FreeMarkerTemplate.class, "/com/sun/xml/ws/test/freemarker");
        cfg.setLocalizedLookup(false);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }

    public FreeMarkerTemplate(String id, int scriptOrder, String workdir, String templateName) {
        this.templateName = templateName;
        root.put("serviceId", id != null ? id : "NULL");
        root.put("stage", scriptOrder);
        root.put("workdir", workdir);
    }

    public void put(String key, Object value) {
        root.put(key, value);
    }

    public String writeFile() {
        String workdir = (String) root.get("workdir");
        String stage = "" + root.get("stage");
        return writeFileTo(workdir, stage + "-" + templateName);
    }

    public String writeFileTo(String dir, String filename) {
        String fullFileName = dir + "/" + filename;
        SourcesCollector.ensureDirectoryExists(new File(fullFileName).getParent());
        System.out.println("\ngenerating file [" + fullFileName + "];\nparametersMap: [");
        Set<Map.Entry> entryset = root.entrySet();
        for(Map.Entry entry : entryset) {
            System.out.print("        ");
            System.out.print(entry.getKey());
            System.out.print(" : ");
            System.out.println(entry.getValue());
        }
        System.out.println("]\n");

        Writer out = null;
        try {
            Template temp = cfg.getTemplate(templateName);

            /* Merge data-model with template */
            out = new FileWriter(fullFileName);
            temp.process(root, out);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (TemplateException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return fullFileName;
    }

}
