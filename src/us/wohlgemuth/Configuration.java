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
import java.net.URL;
import java.util.ArrayList;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class Configuration {

    final private String filename = System.getProperty("user.home") + "/.juno/config.xml";

    public class Site {
        private URL url;
        private ArrayList<String> emailAddresses = new ArrayList<>();

        public Site(URL url, ArrayList<String> emailAddresses){
            this.url=url;
            this.emailAddresses=emailAddresses;
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
    private Integer intervalMinutes;
    private ArrayList<Site> sites = new ArrayList<>();

    public Configuration(){
        if (!isFirstTimeConfiguration()) {
            loadConfigurationFile();
        }
    }

    public ArrayList<Site> getSites() {
        return sites;
    }

    public String getSmtpHost(){
        return smtpHost;
    }

    public String getSmtpUser(){
        return smtpUser;
    }

    public String getSmtpPassword(){
        return smtpPassword;
    }

    public Integer getIntervalMinutes() {
        return intervalMinutes;
    }

    private boolean isFirstTimeConfiguration(){
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
            interval.setAttribute("minutes","60");
            root.appendChild(interval);
            Element smtp = doc.createElement("smtp");
            smtp.setAttribute("host","smtp.gmail.com");
            smtp.setAttribute("user","user@gmail.com");
            smtp.setAttribute("password","password");
            root.appendChild(smtp);
            Element site = doc.createElement("site");
            Element url = doc.createElement("url");
            url.appendChild(doc.createTextNode("https://login.jupitered.com/login/private.php?######-#-##########"));
            Element email1 = doc.createElement("email");
            email1.appendChild(doc.createTextNode("nobody@domain.com"));
            Element email2 = doc.createElement("email");
            email2.appendChild(doc.createTextNode("somebody@domain.com"));
            site.appendChild(url);
            site.appendChild(email1);
            site.appendChild(email1);
            site.appendChild(email2);
            root.appendChild(site);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            Source src = new DOMSource(doc);
            Result dest = new StreamResult(file);
            transformer.transform(src, dest);
        }catch(ParserConfigurationException | TransformerException e) {
            System.err.println("unable to create initial configuration file ["+filename+"]");
            return false;
        }
        System.out.println("created initial configuration file ["+filename+"]");
        return true;
    }

    private void loadConfigurationFile(){
        try {
            File file = new File(filename);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            NodeList nlInterval = doc.getElementsByTagName("interval");
            if (nlInterval.getLength()!=1){
                System.err.println("configuration file must have one interval node");
                return;
            }
            Element eInterval = (Element) nlInterval.item(0);
            String minutes = eInterval.getAttribute("minutes");
            if (null==minutes) {
                System.err.println("inteval node missing required minutes attribute");
                return;
            }
            intervalMinutes = new Integer(minutes);
            NodeList nlSmtp = doc.getElementsByTagName("smtp");
            if (nlSmtp.getLength()!=1){
                System.err.println("configuration file must have one smtp node");
                return;
            }
            Element eSmtp = (Element) nlSmtp.item(0);
            smtpHost = eSmtp.getAttribute("host");
            smtpUser = eSmtp.getAttribute("user");
            smtpPassword = eSmtp.getAttribute("password");
            if ((null==smtpHost) || (null==smtpUser) || (null==smtpPassword)) {
                System.err.println("smtp node missing required attribute(s)");
                return;
            }
            NodeList nlSites = doc.getElementsByTagName("site");
            if (nlSites.getLength()<1){
                System.err.println("configuration file must have at least one site node");
                return;
            }
            for (int i=0; i<nlSites.getLength(); i++){
                Node nSite = nlSites.item(i);
                Element eSite = (Element) nSite;
                NodeList nlUrls = eSite.getElementsByTagName("url");
                NodeList nlEmails = eSite.getElementsByTagName("email");
                if ((nlUrls.getLength()!=1) || (nlEmails.getLength()<1)){
                    System.err.println("missing site elements for site ["+i+"]");
                    continue;
                }
                String strUrl = nlUrls.item(0).getTextContent();
                URL url = new URL(strUrl);
                ArrayList<String> emails = new ArrayList<>();
                for (int j=0; j<nlEmails.getLength(); j++){
                    emails.add(nlEmails.item(j).getTextContent());
                }
                Site site = new Site(url, emails);
                sites.add(site);
            }
        }catch (ParserConfigurationException | IOException | SAXException e){
            e.printStackTrace();
        }
    }
}
