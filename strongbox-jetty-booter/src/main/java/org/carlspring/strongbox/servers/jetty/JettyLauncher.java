
package org.carlspring.strongbox.servers.jetty;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.annotations.AnnotationConfiguration.ClassInheritanceMap;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.eclipse.jetty.webapp.*;

/**
 * @author mtodorov
 */
public class JettyLauncher
{
    private int port;

    private String basedir;

    private String contextPath = "/";

    private String war;

    private Server server;


    public JettyLauncher()
    {
        // TODO: Get this from the strongbox.xml
        port = System.getProperty("strongbox.port") != null ? Integer.parseInt(System.getProperty("strongbox.port")) :
               48080;
    }

    public JettyLauncher(String basedir)
    {
        this.basedir = basedir;
    }

    public Server createExplodedServerInstance() throws ClassNotFoundException, IOException
    {
        Server server = new Server(getPort());

        WebAppContext context = new WebAppContext();
        context.setResourceBase(getBasedir());
        context.setContextPath(getContextPath());

        File baseContextFile = context.getBaseResource().getFile();
        String baseContextPath = baseContextFile.getAbsolutePath();
        context.setDescriptor(baseContextPath + "/WEB-INF/web.xml");
        
        context.setConfigurations(new Configuration[] 
                { 
                    new AnnotationConfiguration(),
                    new WebInfConfiguration(), 
                    new WebXmlConfiguration(),
                    new MetaInfConfiguration(), 
                    new FragmentConfiguration(), 
                    new EnvConfiguration(),
                    new PlusConfiguration(),
                    new JettyWebXmlConfiguration() 
                });

        context.setParentLoaderPriority(true);
        setExtraClassPath(context, baseContextFile);
        
        final ClassInheritanceMap map = new ClassInheritanceMap();
        final ConcurrentHashSet<String> set = new ConcurrentHashSet<>();
        set.add("org.carlspring.strongbox.config.StrongboxWebInitializer");
        map.put("org.springframework.web.WebApplicationInitializer", set);     
        context.setAttribute(AnnotationConfiguration.CLASS_INHERITANCE_MAP, map);
        
        final File dir = new File("temp");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();

        context.setTempDirectory(dir);
        
        server.setHandler(context);

        return server;
    }

    protected void setExtraClassPath(WebAppContext context,
                                     File baseContextFile)
        throws IOException
    {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(baseContextFile.toPath().resolve("WEB-INF/lib"),
                                                                        "spring-web-*.jar"))
        {
            dirStream.forEach(p -> context.setExtraClasspath(p.toAbsolutePath().toString()));
        }
    }

    public Server createWarServerInstance() throws IOException
    {
        Server server = new Server(getPort());

        WebAppContext context = new WebAppContext();
        context.setWar(getWar());
        context.setContextPath(getContextPath());
        context.setParentLoaderPriority(true);
        
        File baseContextFile = context.getBaseResource().getFile();
        String baseContextPath = baseContextFile.getAbsolutePath();
        context.setDescriptor(baseContextPath + "/WEB-INF/web.xml");
        
        context.setConfigurations(new Configuration[] 
                { 
                    new AnnotationConfiguration(),
                    new WebInfConfiguration(), 
                    new WebXmlConfiguration(),
                    new MetaInfConfiguration(), 
                    new FragmentConfiguration(), 
                    new EnvConfiguration(),
                    new PlusConfiguration(), 
                    new JettyWebXmlConfiguration() 
                });
        
        context.setParentLoaderPriority(true);
        setExtraClassPath(context, baseContextFile);
        
        final ClassInheritanceMap map = new ClassInheritanceMap();
        final ConcurrentHashSet<String> set = new ConcurrentHashSet<>();
        set.add("org.carlspring.strongbox.config.StrongboxWebInitializer");
        map.put("org.springframework.web.WebApplicationInitializer", set);      
        context.setAttribute(AnnotationConfiguration.CLASS_INHERITANCE_MAP, map);        
        
        final File dir = new File("target/jetty/tmp");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();

        context.setTempDirectory(dir);
        context.setExtractWAR(true);

        server.setHandler(context);

        return server;
    }

    public void startExplodedInstance()
            throws Exception
    {
        System.out.println("Starting Jetty with exploded webapp...");

        server = createExplodedServerInstance();
        server.start();
        server.join();
    }

    public void startWarInstance()
            throws Exception
    {
        System.out.println("Starting Jetty with WAR (" + new File(getWar()).getCanonicalFile() + ")...");

        server = createWarServerInstance();
        server.start();
        server.join();
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public String getBasedir()
    {
        return basedir;
    }

    public void setBasedir(String basedir)
    {
        this.basedir = basedir;
    }

    public String getContextPath()
    {
        return contextPath;
    }

    public void setContextPath(String contextPath)
    {
        this.contextPath = contextPath;
    }

    public String getWar()
    {
        return war;
    }

    public void setWar(String war)
    {
        this.war = war;
    }

    public Server getServer()
    {
        return server;
    }

    public void setServer(Server server)
    {
        this.server = server;
    }

    public void stopServer()
            throws Exception
    {
        getServer().stop();
        while (!getServer().isStopped())
        {
            Thread.sleep(500);
        }
    }

    public static void main(String[] args)
            throws Exception
    {
        final JettyLauncher launcher = new JettyLauncher();
        launcher.setBasedir("webapp");
        launcher.startExplodedInstance();
    }

}
