package us.wohlgemuth;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class Configuration {

    final private String filename = System.getProperty("user.home") + "/.juno/config.xml";

    public class ConfigurationException extends Exception {
        public ConfigurationException(String msg) {
            super(msg);
        }
    }

    public class Site {
        private URL url;
        private ArrayList<String> emailAddresses = new ArrayList<>();

        public Site(URL url, ArrayList<String> emailAddresses) {
            this.url = url;
            this.emailAddresses = emailAddresses;
        }

        public URL getUrl() {
            return url;
        }

        public ArrayList<String> getemailAddresses() {
            return emailAddresses;
        }
    }

    private String smtpHost;
    private String smtpUser;
    private String smtpPassword;
    private Integer intervalMinutes = 0;
    private ArrayList<Site> sites = new ArrayList<>();

    public Configuration() {
        if (!isFirstTimeConfiguration()) {
            if (!loadConfigurationFile()) {
                throw new RuntimeException("could not load configuration file!");
            }
        }
    }

    public ArrayList<Site> getSites() {
        return sites;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public String getSmtpUser() {
        return smtpUser;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public Integer getIntervalMinutes() {
        return intervalMinutes;
    }

    private boolean isFirstTimeConfiguration() {
        try {
            File file = new File(filename);
            if (file.exists()) return false;
            File dirs = new File(file.getParent());
            dirs.mkdirs();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element root = doc.createElement("configuration");
            doc.appendChild(root);
            Element interval = doc.createElement("interval");
            interval.setAttribute("minutes", "0");
            root.appendChild(interval);
            Element smtp = doc.createElement("smtp");
            smtp.setAttribute("host", "smtp.gmail.com");
            smtp.setAttribute("user", "user@gmail.com");
            smtp.setAttribute("password", "password");
            root.appendChild(smtp);
            Element site = doc.createElement("site");
            site.setAttribute("url", "https://login.jupitered.com/login/private.php?######-#-##########");
            Element email1 = doc.createElement("email");
            email1.setAttribute("address", "nobody@domain.com");
            Element email2 = doc.createElement("email");
            email2.setAttribute("address", "somebody@domain.com");
            site.appendChild(email1);
            site.appendChild(email2);
            root.appendChild(site);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            Source src = new DOMSource(doc);
            Result dest = new StreamResult(file);
            transformer.transform(src, dest);
        } catch (ParserConfigurationException | TransformerException e) {
            System.err.println("unable to create initial configuration file [" + filename + "]");
            return false;
        }
        System.out.println("created initial configuration file [" + filename + "]");
        System.exit(0);  //exit program on creation of new configuration file
        return true;
    }

    private void loadIntervalNode(Document doc) throws ConfigurationException {
        NodeList nlInterval = doc.getElementsByTagName("interval");
        if (nlInterval.getLength() != 1) {
            throw new ConfigurationException("configuration file must have one interval node");
        }
        Element eInterval = (Element) nlInterval.item(0);
        String minutes = eInterval.getAttribute("minutes");
        if ((null == minutes) || minutes.isEmpty()) {
            throw new ConfigurationException("interval node missing required minutes attribute");
        }
        intervalMinutes = new Integer(minutes);
    }

    private void loadSmtpNode(Document doc) throws ConfigurationException {
        NodeList nlSmtp = doc.getElementsByTagName("smtp");
        if (nlSmtp.getLength() != 1) {
            throw new ConfigurationException("configuration file must have one smtp node");
        }
        Element eSmtp = (Element) nlSmtp.item(0);
        smtpHost = eSmtp.getAttribute("host");
        smtpUser = eSmtp.getAttribute("user");
        smtpPassword = eSmtp.getAttribute("password");
        if ((null == smtpHost) || (smtpHost.isEmpty())) {
            throw new ConfigurationException("smtp node missing required host attribute");
        }
        if ((null == smtpUser) || (smtpUser.isEmpty())) {
            throw new ConfigurationException("smtp node missing required user attribute");
        }
        if ((null == smtpPassword) || (smtpPassword.isEmpty())) {
            throw new ConfigurationException("smtp node missing required password attribute");
        }
    }

    private NodeList loadSitesList(Document doc) throws ConfigurationException {
        NodeList nlSites = doc.getElementsByTagName("site");
        if (nlSites.getLength() < 1) {
            throw new ConfigurationException("configuration file must have at least one site node");
        }
        return nlSites;
    }

    private void loadSite(Node nSite) throws ConfigurationException {
        Element eSite = (Element) nSite;
        String strUrl = eSite.getAttribute("url");
        if ((null == strUrl) || strUrl.isEmpty()) {
            throw new ConfigurationException("site node missing required url attribute");
        }
        NodeList nlEmails = eSite.getElementsByTagName("email");
        if (nlEmails.getLength() < 1) {
            throw new ConfigurationException("site node must have at least one email node");
        }
        ArrayList<String> emails = new ArrayList<>();
        for (int j = 0; j < nlEmails.getLength(); j++) {
            Element eEmail = (Element) nlEmails.item(j);
            String strEmail = eEmail.getAttribute("address");
            if ((null == strEmail) || (strEmail.isEmpty())) {
                throw new ConfigurationException("email node missing required address attribute");
            }
            emails.add(strEmail);
        }
        URL url;
        try {
            url = new URL(strUrl);
        } catch (MalformedURLException e) {
            throw new ConfigurationException(e.getMessage());
        }
        Site site = new Site(url, emails);
        sites.add(site);
    }

    private boolean loadConfigurationFile() {
        try {
            File file = new File(filename);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            loadIntervalNode(doc);
            loadSmtpNode(doc);
            NodeList nlSites = loadSitesList(doc);
            for (int i = 0; i < nlSites.getLength(); i++) {
                Node nSite = nlSites.item(i);
                loadSite(nSite);
            }
            if (sites.size() < 1) return false;
        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
            return false;
        } catch (ConfigurationException e) {
            System.err.println(e.getMessage());
            return false;
        }
        return true;
    }
}
