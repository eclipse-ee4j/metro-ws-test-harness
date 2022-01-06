/*
 * Copyright (c) 2012, 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.aether.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;
import org.codehaus.plexus.util.cli.StreamConsumer;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;

/**
 * Executes tests written for WS Test Harness.
 *
 * @author lukas
 */
@Mojo(name = "ws-test", defaultPhase = LifecyclePhase.TEST, requiresProject = false)
public class WSTestMojo extends AbstractMojo {

    private static final String HARNESS_GID = "org.glassfish.metro";
    private static final String HARNESS_AID = "harness-lib";
    private static final String TOPLINK_FACTORY = "com.sun.xml.ws.db.toplink.JAXBContextFactory";
    private static final String SDO_FACTORY = "com.sun.xml.ws.db.sdo.SDOContextFactory";

    public enum Databinding {
        DEFAULT, TOPLINK, SDO;
    }

    public enum Container {
        IN_VM, LWHS, TOMCAT, TOMCAT_LOCAL;
    }

    public enum FI {
        none, pessimistic, optimistic;
    }

    /**
     * Version of Test Harness library to use for running tests.
     */
    @Parameter(defaultValue = "3.0.0")
    private String harnessVersion;

    /**
     * Specify the target JAX-WS version being tested. This determines test
     * exclusions.
     */
    @Parameter(defaultValue = "3.0.0")
    private String version;

    /**
     * Target folder for JUnit test report XMLs.
     *
     * Note: If used from command line (without pom.xml), default is set
     * to <code>results</code> unless <code>-report</code> option is used.
     */
    @Parameter(defaultValue = "${project.build.directory}/surefire-reports")
    private File resultsDirectory;

    /**
     * Find test directories recursively.
     */
    @Parameter(defaultValue = "true")
    private boolean recursive;

    /**
     * Enable all transport dumps.
     */
    @Parameter(defaultValue = "false")
    private boolean dump;

    /**
     * Generate output for debugging harness.
     */
    @Parameter(property = "ws.debug", defaultValue = "false")
    private boolean debug;

    /**
     * Fail the build in case of test errors/failures.
     */
    @Parameter(property = "ws.failonerror", defaultValue = "true")
    private boolean failonerror;

    /**
     * Folder containing test(s).
     */
    @Parameter(property = "ws.test", defaultValue = "${project.basedir}/src/test/testcases")
    private File tests;

    /**
     * Define databinding mode to be used by tests.
     *
     * Supported values:
     * <ul>
     * <li><code>DEFAULT</code>
     * <li><code>TOPLINK</code>
     * </ul>
     */
    @Parameter(defaultValue = "DEFAULT")
    private Databinding databinding;

    /**
     * Define FastInfoset mode to be used by tests.
     *
     * Supported values:
     * <ul>
     * <li><code>none</code>
     * <li><code>optimistic</code>
     * <li><code>pessimistic</code>
     * </ul>
     */
    @Parameter(defaultValue = "none")
    private FI fastinfoset;

    /**
     * Used when running without pom.xml on the command line.
     * Comma (',') is used as a separator.
     */
    @Parameter(property = "args")
    private List<String> args;

    /**
     * Extra JVM options passed to JVM forked by the harness.
     * Use this to pass debugging options to the harness from the command line.
     */
    @Parameter(property = "ws.jvmOpts")
    private String extraVmArgs;

    /**
     * Extra JVM options passed to JVM forked by the harness.
     * Use this to pass options to the harness using plugin configuration in pom.xml.
     */
    @Parameter(property = "vmArgs")
    private List<String> vmArgs;

    /**
     * Directory containing harness extensions.
     */
    @Parameter(property = "ws.extDir", defaultValue = "${project.basedir}/lib/ext")
    private File extDir;

    /**
     * URL from where to get a binary artifact to be tested.
     * This supersedes <code>localImage</code> setting if set.
     */
    @Parameter(property = "ws.imageUrl")
    private String imageUrl;

    /**
     * Path to local binary image to test.
     * This supersedes workspace lookup process if set.
     */
    @Parameter(property = "ws.localImage", defaultValue = "${project.build.directory}/image")
    private File localImage;

    /**
     * Define container mode to be used by tests.
     *
     * Supported values:
     * <ul>
     * <li><code>IN_VM</code>
     * <li><code>LWHS</code>
     * <li><code>TOMCAT</code>
     * </ul>
     */
    @Parameter(property = "ws.transport", defaultValue = "IN_VM")
    private Container transport;

    /**
     * URL from where to get a transport for <code>IN_VM</code> mode tests.
     */
    @Parameter(property = "ws.transportUrl")
    private String transportUrl;

    /**
     * Tomcat home, used only if <code>transport=TOMCAT</code> is set.
     */
    @Parameter(property = "tomcat.home")
    private File tomcatHome;

    /**
     * Misc files required by tests. For example JKSs for WSIT tests.
     */
    @Parameter(defaultValue = "${project.basedir}/misc")
    private File wsitConf;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}",
            readonly = true)
    private List<RemoteRepository> remoteRepos;

    @Parameter(defaultValue="${project}", readonly=true)
    private MavenProject project;

    @Parameter(defaultValue="${settings}", readonly=true )
    private Settings settings;

    @Component
    private ArchiverManager archiverManager;

    /**
     * The entry point to Aether.
     */
    @Component
    private RepositorySystem repoSystem;

    /**
     * The current repository/network configuration of Maven.
     */
    @Parameter(defaultValue = "${repositorySystemSession}",
            readonly = true)
    private RepositorySystemSession repoSession;

    private File imageRoot = null;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File imageFolder = new File(project.getBuild().getDirectory(), "tested-image");
        try {
            URL u = new URL(imageUrl);
            try {
                imageFolder.mkdirs();
                imageRoot = prepareImage(u, imageFolder);
                getLog().info("testing downloaded image (" + imageRoot.getPath() +")...");
            } catch (NoSuchArchiverException ex) {
                throw new MojoExecutionException(ex.getMessage(), ex);
            } catch (IOException ex) {
                throw new MojoFailureException(ex.getMessage(), ex);
            }
        } catch (MalformedURLException muex) {
        }
        if (imageRoot == null) {
            if (localImage.exists() && localImage.isDirectory() && findImageRoot(localImage) != null) {
                imageRoot = findImageRoot(localImage);
                getLog().info("testing local image (" + imageRoot.getPath() +")...");
            } else {
                getLog().info("testing local workspace...");
            }
        } else {
            getLog().info("testing local workspace...");
        }

        Commandline cmd = new Commandline();
        cmd.setExecutable(new File(new File(System.getProperty("java.home"), "bin"), getJavaExec()).getAbsolutePath());
        cmd.setWorkingDirectory(project.getBasedir());

        // lukas:
        // Some jar files on the classpath may be multi-release jars.
        // To allow AntClassLoader (used by Harness) to find correct class files
        // in these jars, either make sure that Ant 1.10.6+ is used at runtime
        // or force JDK to always create JarFile instances with Runtime.Version
        // of the JDK being used.
        // Since the former requirement is hard to guarantee and to prevent false
        // alarms when running tests, let's just force JDK to use the right
        // JarFile constructor ourselves.
        // for more details, see: https://bz.apache.org/bugzilla/show_bug.cgi?id=62952
        // see also com.sun.xml.ws.test.Realm.getClassLoader()
        cmd.createArg().setLine("-Djdk.util.jar.enableMultiRelease=force");

        if (extDir != null && extDir.exists() && extDir.isDirectory()) {
            cmd.createArg().setValue("-DHARNESS_EXT=" + extDir.getAbsolutePath());
        } else {
            getLog().info("'ext' directory not found");
        }

        if (System.getProperty("net.sourceforge.cobertura.datafile") != null) {
            cmd.createArg().setValue("-Dnet.sourceforge.cobertura.datafile=" + System.getProperty("net.sourceforge.cobertura.datafile"));
        }

        if (wsitConf != null && wsitConf.exists() && wsitConf.isDirectory()) {
            cmd.createArg().setValue("-DWSIT_HOME=" + wsitConf.getAbsolutePath());
        }

        if (isFastInfosetEnabled()) {
            switch (fastinfoset) {
                case optimistic:
                    cmd.createArg().setLine("-Dcom.sun.xml.ws.client.ContentNegotiation=optimistic");
                    break;
                case pessimistic:
                    cmd.createArg().setLine("-Dcom.sun.xml.ws.client.ContentNegotiation=pessimistic");
                    break;
                default:
                    //none: cmd.createArg().setLine("-Dcom.sun.xml.ws.client.ContentNegotiation=none");
                    break;
            }
        }

        if (isToplink()) {
            cmd.createArg().setLine("-DBindingContextFactory=" + TOPLINK_FACTORY);
        } else if (isSDO()) {
            cmd.createArg().setLine("-DBindingContextFactory=" + SDO_FACTORY);
        }

        if (extraVmArgs != null && extraVmArgs.trim().length() > 1) {
            cmd.createArg().setLine(extraVmArgs);
        }

        if (settings != null) {
            for (Proxy p : settings.getProxies()) {
                if (p.isActive()) {
                    String protocol = p.getProtocol().trim();
                    if (p.getHost() != null) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("-D");
                        sb.append(protocol);
                        sb.append(".proxyHost=");
                        sb.append(p.getHost());
                        cmd.createArg().setLine(sb.toString());
                    }
                    if (p.getPort() > -1) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("-D");
                        sb.append(protocol);
                        sb.append(".proxyPort=");
                        sb.append(p.getPort());
                        cmd.createArg().setLine(sb.toString());
                    }
                    if (p.getNonProxyHosts() != null) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("-D");
                        sb.append(protocol);
                        sb.append(".nonProxyHosts=\"");
                        sb.append(p.getNonProxyHosts().trim());
                        sb.append("\"");
                        cmd.createArg().setValue(sb.toString());
                    }
                }
            }
        }

        if (vmArgs != null) {
            for (String arg : vmArgs) {
                if (arg.contains("-DBindingContextFactory=") && (isToplink() || isSDO())) {
                    String line = removeVmArg(arg, "BindingContextFactory");
                    if (line != null) {
                        cmd.createArg().setLine(line.trim());
                    } else {
                        getLog().info("removing '" + arg + "' from default configuration.");
                    }
                } else if (arg.contains("-Dcom.sun.xml.ws.client.ContentNegotiation=") && isFastInfosetEnabled()) {
                    String line = removeVmArg(arg, "com.sun.xml.ws.client.ContentNegotiation");
                    if (line != null) {
                        cmd.createArg().setLine(line.trim());
                    } else {
                        getLog().info("removing '" + arg + "' from default configuration.");
                    }
                } else {
                    cmd.createArg().setLine(arg);
                }
            }
        }

        cmd.createArg().setLine("-cp " + getHarnessClassPath());
        cmd.createArg().setValue("com.sun.xml.ws.test.Main");
        if (project.getFile() != null) {
            cmd.createArg().setLine("-report " + resultsDirectory.getAbsolutePath());
        } else if (args != null && !args.contains("-report")) {
            cmd.createArg().setLine("-report results");
        }

        if (recursive) {
            cmd.createArg().setValue("-r");
        }
        if (debug) {
            cmd.createArg().setValue("-debug");
        }
        if (dump) {
            cmd.createArg().setValue("-dump");
        }
        if (version != null && version.trim().length() > 0) {
            cmd.createArg().setLine("-version " + version);
        }

        switch (transport) {
            case IN_VM:
                if (imageRoot != null) {
                    File transportFile;
                    try {
                        transportFile = download(new URL(transportUrl), imageFolder);
                    } catch (IOException ex) {
                        transportFile = new File(project.getBuild().getDirectory(), "test-lib/jaxws-local-transport.jar");
                        if (!transportFile.exists()) {
                            throw new MojoExecutionException("Cannot find local transport jar.  Set 'transportUrl' in plugin's configuration.", ex);
                        }
                    }
                    cmd.createArg().setLine("-transport " + transportFile.getAbsolutePath());
                }
                break;
            case LWHS:
                cmd.createArg().setLine("-lwhs");
                break;
            case TOMCAT:
                if (tomcatHome == null) {
                    throw new MojoExecutionException("'tomcat.home' is not set.");
                }
                cmd.createArg().setLine("-tomcat-embedded " + tomcatHome.getAbsolutePath());
                break;
            case TOMCAT_LOCAL:
                if (tomcatHome == null) {
                    throw new MojoExecutionException("'tomcat.home' is not set.");
                }
                cmd.createArg().setLine("-tomcat-local " + tomcatHome.getAbsolutePath());
                break;
        }

        List<String> filters = new ArrayList<String>();
        if (imageRoot != null) {
            if (isJaxWsRIRoot(imageRoot)) {
                cmd.createArg().setLine("-cp:jaxws-image " + imageRoot.getAbsolutePath());
            } else if (isMetroRoot(imageRoot)) {
                cmd.createArg().setLine("-cp:wsit-image " + imageRoot.getAbsolutePath());
            } else {
                throw new MojoExecutionException("Unknown/Unsupported image: " + imageRoot);
            }
            if (isToplink()) {
                cmd.createArg().setLine("-cp:override " + getToplinkCP(imageRoot));
                filters.add("-cp:override");
            } else if (isSDO()) {
                cmd.createArg().setLine("-cp:override " + getSdoCP(imageRoot));
                filters.add("-cp:override");
            }
            filters.add("-cp:jaxws");
            filters.add("-cp:wsit");
        }

        if (args != null) {
            for (String arg : args) {
                if (filters.isEmpty()) {
                    cmd.createArg().setLine(arg);
                } else {
                    boolean found = false;
                    for (String filter : filters) {
                        if (arg.contains(filter)) {
                            getLog().info("removing '" + arg + "' from default configuration.");
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        cmd.createArg().setLine(arg);
                    }
                }
            }
        }

        cmd.createArg().setLine(tests.getAbsolutePath());

        if (debug) {
            getLog().info(cmd.toString());
        }
        getLog().debug(cmd.toString());

        StreamConsumer sc = new DefaultConsumer();
        try {
            int i = CommandLineUtils.executeCommandLine(cmd, sc, sc);
            if (i > 0 && failonerror) {
                throw new MojoFailureException(i + " test(s) failed");
            } else if (i < 0) {
                throw new MojoExecutionException("Invalid command: " + cmd.toString());
            }
        } catch (CommandLineException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private File download(URL u, File destDir) throws IOException {
        getLog().info("Downloading: " + u);
        File imageZip = new File(destDir, new File(u.getFile()).getName());
        if (imageZip.createNewFile()) {
            getLog().info("to: " + imageZip.getAbsolutePath());
            IOUtil.copy(u.openStream(), new FileOutputStream(imageZip));
        }
        return imageZip;
    }

    private File prepareImage(URL u, File destDir) throws IOException, NoSuchArchiverException {
        File zip = download(u, destDir);
        getLog().info("unpacking " + zip.getName() + "...");
        UnArchiver unArchiver = archiverManager.getUnArchiver(zip);
        unArchiver.setSourceFile(zip);
        unArchiver.setDestDirectory(destDir);
        unArchiver.extract();
        return findImageRoot(destDir);
    }

    private File findImageRoot(File dir) {
        File f = new File(dir, "jaxws-ri");
        if (f.exists() && f.isDirectory()) {
            return f;
        }
        f = new File(dir, "metro");
        if (f.exists() && f.isDirectory()) {
            return f;
        }
        return null;
    }

    private String getJavaExec() {
        return Os.isFamily(Os.FAMILY_WINDOWS) ? "java.exe" : "java";
    }

    private String getHarnessClassPath() throws MojoExecutionException {
        StringBuilder sb = new StringBuilder();
        for (Artifact a : getHarnessLib()) {
            sb.append(a.getFile().getAbsolutePath());
            sb.append(File.pathSeparator);
        }
        return sb.substring(0, sb.length() - 1);
    }

    private Set<Artifact> getHarnessLib() throws MojoExecutionException {
       org.eclipse.aether.artifact.DefaultArtifact harnessLib = new org.eclipse.aether.artifact.DefaultArtifact(HARNESS_GID,HARNESS_AID, null, "jar", harnessVersion);
       Set<Artifact> result = new HashSet<>();
       Set<ArtifactRequest> dependenciesRequest = new HashSet<>();
       ArtifactRequest request = new ArtifactRequest();
       request.setArtifact(harnessLib);
       request.setRepositories(remoteRepos);
       dependenciesRequest.add(request);

       List<ArtifactResult> resolvedDependencies;
       try {
           resolvedDependencies = repoSystem.resolveArtifacts(repoSession, dependenciesRequest);
       } catch (ArtifactResolutionException ex) {
           throw new MojoExecutionException(ex.getMessage(), ex);
        }

        List<Dependency> artifacts = new ArrayList<>();
        for (ArtifactResult dependency : resolvedDependencies) {
            Artifact a = dependency.getArtifact();
            result.add(a);
            artifacts.add(new Dependency(dependency.getArtifact(), "compile"));
        }

        DependencyResult res = null;
        try {
            CollectRequest cr = new CollectRequest(artifacts.iterator().next(), remoteRepos);
            DependencyRequest dr = new DependencyRequest();
            dr.setCollectRequest(cr);
            res = repoSystem.resolveDependencies(repoSession, dr);
        } catch (DependencyResolutionException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }

        for (ArtifactResult dependency : res.getArtifactResults()) {
            Artifact a = dependency.getArtifact();
            if (getLog().isDebugEnabled()) {
                getLog().debug("got dependency: " + a.getGroupId() + ":" + a.getArtifactId() + ":" + a.getVersion());
            }
            result.add(dependency.getArtifact());
        }

        return result;
    }

    private boolean isToplink() {
        return Databinding.TOPLINK == databinding;
    }

    private boolean isSDO() {
        return Databinding.SDO == databinding;
    }

    private boolean isFastInfosetEnabled() {
        return fastinfoset != null && FI.none != fastinfoset;
    }

    private String getCP(File root, String... paths) {
        StringBuilder sb = new StringBuilder();
        for (String p : paths) {
            File item = new File(root, p);
            if (!item.exists()) {
                getLog().warn(item.getAbsolutePath() + " does not exist");
            } else {
            sb.append(item.getAbsolutePath());
            sb.append(File.pathSeparatorChar);
            }
        }
        return sb.substring(0, sb.length() - 1);
    }

    private String getToplinkCP(File root) throws MojoExecutionException {
        if (isJaxWsRIRoot(root)) {
            return getCP(root,
                    "lib/plugins/jaxws-eclipselink-plugin.jar",
                    "lib/plugins/org.eclipse.persistence.asm.jar",
                    "lib/plugins/org.eclipse.persistence.core.jar",
                    "lib/plugins/org.eclipse.persistence.moxy.jar",
                    "lib/plugins/eclipselink.jar"
            );
        } else if (isMetroRoot(root)) {
            return getCP(root, "lib/databinding/jaxws-eclipselink-plugin.jar") +
                    File.pathSeparatorChar +
                    getCP(new File(project.getBuild().getDirectory()), "test-lib/eclipselink.jar");
        }
        throw new MojoExecutionException("Unknown/Unsupported image: " + imageRoot);
    }

    private String getSdoCP(File root) throws MojoExecutionException {
        if (isJaxWsRIRoot(root)) {
            return getCP(root,
                    "lib/plugins/sdo-eclipselink-plugin.jar",
                    "lib/plugins/org.eclipse.persistence.asm.jar",
                    "lib/plugins/org.eclipse.persistence.core.jar",
                    "lib/plugins/org.eclipse.persistence.moxy.jar",
                    "lib/plugins/org.eclipse.persistence.sdo.jar",
                    "lib/plugins/eclipselink.jar"
            );
        } else if (isMetroRoot(root)) {
            return getCP(root, "lib/databinding/sdo-eclipselink-plugin.jar") +
                    File.pathSeparatorChar +
                    getCP(new File(project.getBuild().getDirectory()),
                            "test-lib/eclipselink.jar"
                            );
        }
        throw new MojoExecutionException("Unknown/Unsupported image: " + imageRoot);
    }

    private boolean isJaxWsRIRoot(File root) {
        return "jaxws-ri".equals(root.getName());
    }

    private boolean isMetroRoot(File root) {
        return "metro".equals(root.getName());
    }

    private String removeVmArg(String line, String prop) {
        String[] opts = line.split(" ");
        if (opts.length > 1) {
            StringBuilder sb = new StringBuilder();
            for (String opt : opts) {
                if (opt.trim().startsWith("-D" + prop + "=")) {
                    getLog().info("removing '" + opt + "' from default configuration.");
                    continue;
                }
                sb.append(opt);
                sb.append(" ");
            }
            return sb.toString();
        }
        return null;
    }

}
