/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test.container.cargo;

import com.sun.xml.ws.test.World;
import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.container.WAR;
import com.sun.xml.ws.test.container.cargo.gf.GlassfishPropertySet;
import com.sun.xml.ws.test.tool.WsTool;
import com.sun.xml.ws.test.util.FileUtil;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.generic.AbstractFactoryRegistry;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.ConfigurationFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.tomcat.TomcatPropertySet;

/**
 * {@link ApplicationContainer} that launches a container from within the harness.
 *
 * <p>
 * This uses an image of the container installed locally, but
 * this operation does not affect the data file and configuration
 * files in that installation, so you need not have an installation
 * dedicated to this test harness.
 *
 * @author Kohsuke Kawaguchi
 */
public class InstalledCargoApplicationContainer extends AbstractRunnableCargoContainer<InstalledLocalContainer> {
    /**
     *
     * @param containerId
     *      The ID that represents the container. "tomcat5x" for Tomcat.
     * @param homeDir
     *      The installation of the container. For Tomcat, this is
     */
    public InstalledCargoApplicationContainer(WsTool wsimport, WsTool wsgen, String containerId, File homeDir, int port, boolean httpspi) throws IOException {
        super(wsimport,wsgen,port,httpspi);

        // needed until glassfish becomes a part of Cargo

        ConfigurationFactory configurationFactory = new DefaultConfigurationFactory(AbstractFactoryRegistry.class.getClassLoader());
        // For tomcat local, cargo doesn't copy shared/lib jars to working dir
        // configurationFactory.registerConfiguration("tomcat5x", ContainerType.INSTALLED, ConfigurationType.STANDALONE, Tomcat5xMetroStandaloneLocalConfiguration.class);

        DefaultContainerFactory containerFactory = new DefaultContainerFactory(AbstractFactoryRegistry.class.getClassLoader());

        File containerWorkDir = FileUtil.createTmpDir(true);
        containerWorkDir.mkdirs();
        System.out.println("Container working directory: "+containerWorkDir);

        LocalConfiguration configuration =
            (LocalConfiguration) configurationFactory.createConfiguration(
                containerId, ContainerType.INSTALLED, ConfigurationType.STANDALONE,
                containerWorkDir.getAbsolutePath());

        configuration.setProperty(ServletPropertySet.PORT, Integer.toString(httpPort));
        configuration.setProperty(GeneralPropertySet.RMI_PORT, getPort());
//        configuration.setLogger(new SimpleLogger());

        // In case this is Glassfish, override all the other TCP ports
        // so that multiple test runs can co-exist on the same machine
        configuration.setProperty(GlassfishPropertySet.JMS_PORT,                getPort());
        configuration.setProperty(GlassfishPropertySet.IIOP_PORT,               getPort());
        configuration.setProperty(GlassfishPropertySet.HTTPS_PORT,              getPort());
        configuration.setProperty(GlassfishPropertySet.IIOPS_PORT,              getPort());
        configuration.setProperty(GlassfishPropertySet.IIOP_MUTUAL_AUTH_PORT,   getPort());
        configuration.setProperty(GlassfishPropertySet.JMX_ADMIN_PORT,          getPort());
        configuration.setProperty(GlassfishPropertySet.ADMIN_PORT,              getPort());

        if (containerId.startsWith("tomcat")) {
            configuration.setProperty(TomcatPropertySet.AJP_PORT, getPort());

            if (Boolean.getBoolean("harness.useSSL")) {
                configuration.setProperty(GeneralPropertySet.PROTOCOL, "https");
                configuration.setProperty(TomcatPropertySet.HTTP_SECURE, "true");
                configuration.setProperty(TomcatPropertySet.CONNECTOR_KEY_STORE_FILE, new File(System.getProperty("harness.ssl.home"), "server-keystore.jks").getAbsolutePath());
                configuration.setProperty(TomcatPropertySet.CONNECTOR_KEY_STORE_PASSWORD, "changeit");
                configuration.setProperty(TomcatPropertySet.CONNECTOR_KEY_STORE_TYPE, "JKS");
                configuration.setProperty(TomcatPropertySet.CONNECTOR_TRUST_STORE_FILE, new File(System.getProperty("harness.ssl.home"), "server-truststore.jks").getAbsolutePath());
                configuration.setProperty(TomcatPropertySet.CONNECTOR_TRUST_STORE_PASSWORD, "changeit");
                configuration.setProperty(TomcatPropertySet.CONNECTOR_TRUST_STORE_TYPE, "JKS");

                HostnameVerifier hv = new HostnameVerifier() {
                    public boolean verify(String urlHostName, SSLSession session) {
                        System.out.println("Warning: got: '" + urlHostName
                                + "' expected '" + session.getPeerHost() + "'");
                        return true;
                    }
                };
                HttpsURLConnection.setDefaultHostnameVerifier(hv);
            }
        }

        // TODO: we should provide a mode to launch the container with debugger

        container = (InstalledLocalContainer) containerFactory.createContainer(
            containerId, ContainerType.INSTALLED, configuration);
        container.setHome(homeDir.getAbsolutePath());
        container.setOutput(containerWorkDir.getAbsolutePath() + File.separatorChar + "server.log");

        Map<String, String> props = new HashMap<String, String>();
        props.put("java.endorsed.dirs", System.getProperty("java.endorsed.dirs"));
        props.put("WSIT_HOME", System.getProperty("WSIT_HOME"));
//        props.put("com.sun.xml.ws.transport.local.LocalTransportTube.dump", "true");
//        props.put("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
//        props.put("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
        container.setSystemProperties(props);

        for (File f : World.runtime.list()) {
            if (f.getName().endsWith(".jar")) {
                container.addExtraClasspath(f.getAbsolutePath());
            } else {
                if ("classes".equals(f.getName())) {
                    String fName = f.getParentFile().getParentFile().getName();
                    File jar = new File(new File(System.getProperty("java.io.tmpdir")), fName + System.currentTimeMillis() + ".jar");
                    jar.deleteOnExit();
                    Zip zip = new Zip();
                    zip.setProject(new Project());
                    zip.setDestFile(jar);
                    zip.setBasedir(f);
                    zip.execute();
                    container.addExtraClasspath(jar.getAbsolutePath());
                }
            }
        }

    }

    /**
     * For tomcat local, since cargo doesn't support copying of shared/lib jars,
     * the war can be created with the jars in the WEB-INF/lib. Other option is
     * to subclass Tomcat5xStandaloneLocalConfiguration and do the copying of
     * jars from Tomcat installation to local working dir in doConfigure()
     *
     * Copy JAX-WS runtime code?
     *
    protected boolean copyRuntimeLibraries() {
        return false;
    }
     */

    @Override
    public String toString() {
        return "CargoLocalContainer:"+container.getId();
    }

    private String getPort() {
        return String.valueOf(AbstractRunnableCargoContainer.getFreePort());
    }

    @Override
    protected WAR assembleWar(DeployedService service) throws Exception {
        WAR war = super.assembleWar(service);
        if (service.service.isSTS) {
            updateWsitClient(war, service, getServiceUrl(service.service.getGlobalUniqueName()).toExternalForm());
        }
        return war;
    }


}
