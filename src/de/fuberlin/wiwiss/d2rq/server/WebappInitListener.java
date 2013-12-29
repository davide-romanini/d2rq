package de.fuberlin.wiwiss.d2rq.server;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.fuberlin.wiwiss.d2rq.SystemLoader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Initialize D2R server on startup of an appserver such as Tomcat. This listener should
 * be included in the web.xml. This is compatible with Servlet 2.3 spec compliant appservers.
 *
 * @author Inigo Surguy
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class WebappInitListener implements ServletContextListener {
	private final static Log log = LogFactory.getLog(WebappInitListener.class);
	
	public void contextInitialized(ServletContextEvent event) {
		ServletContext context = event.getServletContext();

		// Is there already a loader in the servlet context, put there
		// by the JettyLauncher?
		SystemLoader loader = D2RServer.retrieveSystemLoader(context);
		if (loader == null) {
			log.info("Fresh ServletContext; initializing a new SystemLoader");
			// We are running as pure webapp, without JettyLauncher,
			// so initalize the loader from the configFile context parameter.
			loader = new SystemLoader();
			D2RServer.storeSystemLoader(loader, context);
			if (context.getInitParameter("configFile") == null) {
				throw new RuntimeException("No configFile configured in web.xml");
			}
			String configFileName = absolutize(context.getInitParameter("configFile"), context);
			loader.setMappingURL(configFileName);
			loader.setResourceStem("resource/");
		}
		D2RServer server = loader.getD2RServer();
		server.start();
		VelocityWrapper.initEngine(server, context);
	}

	public void contextDestroyed(ServletContextEvent event) {
		D2RServer server = D2RServer.fromServletContext(event.getServletContext());
		if (server != null)
			server.shutdown();
	}
	
	private String absolutize(String fileName, ServletContext context) {
                String origName = fileName;
		if (!fileName.matches("[a-zA-Z0-9]+:.*")) {
			fileName = context.getRealPath("WEB-INF/" + fileName);
		}
                // ServletContext.getRealPath isn't guaranteed to be not null
                if (fileName == null) {
                    File tmpFile = null; 
                    try {
                        InputStream i = context.getResourceAsStream("WEB-INF/" + origName);
                        File tmpDir = (File) context.getAttribute("javax.servlet.context.tempdir");
                        tmpFile = File.createTempFile("__c", origName, tmpDir);

                        OutputStream out = new FileOutputStream(tmpFile);
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = i.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                        out.close();
                        i.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    log.info("Saved " + fileName + " into " + tmpFile);
                    fileName = tmpFile.getAbsolutePath();
                }
		return ConfigLoader.toAbsoluteURI(fileName);
	}
}
