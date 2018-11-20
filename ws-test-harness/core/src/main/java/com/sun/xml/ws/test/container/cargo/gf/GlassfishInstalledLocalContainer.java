/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.container.cargo.gf;

import com.sun.xml.ws.test.World;
import com.sun.xml.ws.test.util.FileUtil;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteWatchdog;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.codehaus.cargo.container.ContainerCapability;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.container.spi.AbstractInstalledLocalContainer;
import org.codehaus.cargo.util.CargoException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.cargo.container.spi.jvm.JvmLauncher;

/**
 * @author Kohsuke Kawaguchi
 */
public class GlassfishInstalledLocalContainer extends AbstractInstalledLocalContainer {
    public GlassfishInstalledLocalContainer(LocalConfiguration localConfiguration) {
        super(localConfiguration);
    }

    /**
     * Invokes asadmin
     */
    /*package*/ void invokeAsAdmin(boolean async, String... args) {
        File exec = getAsadminExecutable();

        List<String> cmds = new ArrayList<String>();
        cmds.add(exec.getAbsolutePath());
        for (String arg : args) {
            cmds.add(arg);
        }

        try {
            Execute exe = new Execute(new PumpStreamHandler(),new ExecuteWatchdog(30*1000L));
            exe.setAntRun(World.project);
            exe.setCommandline(cmds.toArray(new String[0]));
            if(async) {
                exe.spawn();
            } else {
                int exitCode = exe.execute();
                if(exitCode!=0)
                    // the first token is the command
                    throw new CargoException(cmds+" failed. asadmin exited "+exitCode);
            }
        } catch (IOException e) {
            throw new CargoException("Failed to invoke asadmin",e);
        }
    }

    private File getAsadminExecutable() {
        String home = getHome();
        if(home==null || !new File(home).exists())
            throw new CargoException("Glassfish home directory is not set");

        File exec;

        if(File.pathSeparatorChar==';') {
            // on Windows
            exec = new File(new File(home),"bin/asadmin.bat");
        } else {
            // on other systems
            exec = new File(new File(home),"bin/asadmin");
        }

        if(!exec.exists())
            throw new CargoException("asadmin command not found at "+exec);

        return exec;
    }

    protected void doStart(Java java) throws Exception {
        getConfiguration().configure(this);

        System.out.println("Starting domain on HTTP port "+
            getConfiguration().getPropertyValue(ServletPropertySet.PORT)+" and admin port "+
            getConfiguration().getPropertyValue(GlassfishPropertySet.ADMIN_PORT));

        // see https://glassfish.dev.java.net/issues/show_bug.cgi?id=885
        // needs to spawn
        invokeAsAdmin(true, "start-domain",
            "--interactive=false",
            "--domaindir",
            new File(getConfiguration().getHome()).getAbsolutePath(),
            "cargo-domain-" + getConfiguration().getPropertyValue(ServletPropertySet.PORT)
            );

        // to workaround GF bug, the above needs to be async,
        // so give it some time to make the admin port available
        Thread.sleep(20*1000);

        // deploy scheduled deployables
        GlassfishInstalledLocalDeployer deployer = new GlassfishInstalledLocalDeployer(this);
        for (Deployable deployable : (List<Deployable>)getConfiguration().getDeployables()) {
            deployer.deploy(deployable);
        }
    }

    protected void doStop(Java java) throws Exception {
        invokeAsAdmin(false, "stop-domain",
            "--domaindir",
            new File(getConfiguration().getHome()).getAbsolutePath(),
            "cargo-domain-" + getConfiguration().getPropertyValue(ServletPropertySet.PORT)
            );
        FileUtil.deleteRecursive(new File(getConfiguration().getHome()));
    }

    public String getId() {
        return "glassfish1x";
    }

    public String getName() {
        return "Glassfish v1";
    }

    public ContainerCapability getCapability() {
        return CAPABILITY;
    }

    private static final ContainerCapability CAPABILITY = new GlassfishContainerCapability();

    @Override
    protected void doStart(JvmLauncher jl) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void doStop(JvmLauncher jl) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
