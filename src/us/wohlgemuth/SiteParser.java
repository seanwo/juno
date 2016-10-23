package us.wohlgemuth;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class SiteParser {

    public class SiteException extends Exception {
        public SiteException(String msg) {
            super(msg);
        }
    }

    private URL url;

    private final String SiteCookieName = "__cfduid";

    public SiteParser(URL url) {
        this.url = url;
    }

    private Connection connect(URL url, Connection.Method method) {
        Connection connection = Jsoup.connect(url.toExternalForm());
        connection.method(method);
        return connection;
    }

    private Connection.Response getResponse(Connection connection) throws SiteException {
        Connection.Response response;
        try {
            response = connection.execute();
        } catch (IOException e) {
            e.printStackTrace();
            throw new SiteException("unable to retrieve page");
        }
        return response;
    }

    private String getCookie(Connection.Response response, String name) throws SiteException {
        String cookie = response.cookie(name);
        if ((null == cookie) || (cookie.isEmpty())) {
            throw new SiteException("unable to acquire cookie");
        }
        return cookie;
    }

    private void setCookie(Connection connection, String cookieName, String cookieValue) {
        connection.cookie(cookieName, cookieValue);
    }

    private Document getDocument(Connection.Response response) throws SiteException {
        Document doc;
        try {
            doc = response.parse();
        } catch (IOException e) {
            e.printStackTrace();
            throw new SiteException("unable to parse response");
        }
        return doc;
    }

    private Document navigate(URL url, String cookie, HashMap<String, String> params) throws SiteException {
        Connection connection = connect(url, Connection.Method.POST);
        setCookie(connection, SiteCookieName, cookie);
        setFormParams(connection, params);
        Connection.Response response = getResponse(connection);
        Document document = getDocument(response);
        return document;
    }

    private Element getFormElement(Document document) throws SiteException {
        Elements eForms = document.getElementsByTag("form");
        if (eForms.size() != 1) {
            throw new SiteException("unable to determine proper <form> element on page");
        }
        Element eForm = eForms.first();
        return eForm;
    }

    private URL getFormURL(Document document) throws SiteException {
        Element eForm = getFormElement(document);
        String action = eForm.attr("action");
        if (action.isEmpty()) {
            throw new SiteException("form does not contain action attribute");
        }
        URL urlForm;
        try {
            urlForm = new URL(url, action);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new SiteException("unable to construct webform url");
        }
        return urlForm;
    }

    private String getSchoolYear(Element form) throws SiteException {
        Elements elements = form.getElementsByAttributeValue("name", "schoolyear");
        if (elements.size() != 1) {
            throw new SiteException("unable to determine school year");
        }
        Element element = elements.get(0);
        Elements selected = element.getElementsByAttribute("selected");
        if (selected.size() != 1) {
            throw new SiteException("unable to determine school year");
        }
        Element schoolYear = selected.get(0);
        String value = schoolYear.attr("value");
        if (selected.isEmpty()) {
            throw new SiteException("unable to determine school year");
        }
        return value;
    }

    private HashMap<String, String> getDefaultFormParams(Document document) throws SiteException {
        Element eForm = getFormElement(document);
        Elements inputElements = eForm.getElementsByTag("input");
        if (inputElements.isEmpty()) {
            throw new SiteException("unable to find webform input values");
        }
        HashMap<String, String> params = new HashMap<>();
        for (Element e : inputElements) {
            String type = e.attr("type");
            String name = e.attr("name");
            String value = e.attr("value");
            if ((!name.isEmpty()) && (type.toLowerCase().compareTo("hidden") == 0)) {
                params.put(name, value);
            }
        }
        String schoolYear = getSchoolYear(eForm);
        params.put("schoolyear", schoolYear);
        params.put("to", "grades");
        return params;
    }

    private void setFormParams(Connection connection, HashMap<String, String> params) {
        connection.data(params);
    }

    private HashMap<Integer, String> getClasses(Document document) throws SiteException {
        Elements elements = document.getElementsByAttributeValue("name", "classmenu");
        if (elements.size() != 1) {
            throw new SiteException("unable to determine classes");
        }
        Element element = elements.get(0);
        Elements eClasses = element.getElementsByTag("option");
        HashMap<Integer, String> classes = new HashMap<>();
        for (Element e : eClasses) {
            String value = e.attr("value");
            String name = e.text();
            if ((!value.isEmpty())) {
                classes.put(new Integer(value), name);
            }
        }
        return classes;
    }

    private String getTerm(Document document) throws SiteException {
        Elements elements = document.getElementsByAttributeValue("name", "termmenu");
        if (elements.size() != 1) {
            throw new SiteException("unable to determine current term");
        }
        Element element = elements.get(0);
        Elements eTerms = element.getElementsByAttribute("selected");
        if (eTerms.size() != 1) {
            throw new SiteException("unable to determine current term");
        }
        return eTerms.get(0).text();
    }

    private String getStudentName(Document document) throws SiteException {
        Elements elements = document.getElementsByAttributeValue("name", "changestud");
        if (elements.size() != 1) {
            throw new SiteException("unable to determine current student");
        }
        Element element = elements.get(0);
        Elements eTerms = element.getElementsByAttribute("selected");
        if (eTerms.size() != 1) {
            throw new SiteException("unable to determine current student");
        }
        return eTerms.get(0).text();
    }

    private String getAssignments(Document document) {
        StringBuilder assignmentBuilder = new StringBuilder();
        Elements eAssignments = document.getElementsByAttributeValueStarting("onclick", "goassign");
        for (Element eAssignment : eAssignments) {
            for (Element child : eAssignment.children()) {
                assignmentBuilder.append(child.text());
            }
            assignmentBuilder.append("\n");
        }
        return assignmentBuilder.toString();
    }

    private HashMap<Integer, String> getAssignmentBlobs(Set<Integer> classes, URL url, String cookie, HashMap<String, String> params) throws SiteException {
        HashMap<Integer, String> assignmentBlobs = new HashMap<>();
        Iterator<Integer> it = classes.iterator();
        while (it.hasNext()) {
            Integer classId = it.next();
            params.put("class1", classId.toString());
            Document document = navigate(url, cookie, params);
            String assignments = getAssignments(document);
            assignmentBlobs.put(classId, assignments);
        }
        return assignmentBlobs;
    }

    public StudentData getStudentData() {
        StudentData student = null;
        try {
            Connection connection = connect(url, Connection.Method.GET);
            Connection.Response response = getResponse(connection);
            String cookie = getCookie(response, SiteCookieName);
            Document document = getDocument(response);
            URL formUrl = getFormURL(document);
            HashMap<String, String> params = getDefaultFormParams(document);
            document = navigate(formUrl, cookie, params);
            String studentName = getStudentName(document);
            String term = getTerm(document);
            HashMap<Integer, String> classes = getClasses(document);
            HashMap<Integer, String> assignmentBlobs = getAssignmentBlobs(classes.keySet(), formUrl, cookie, params);
            student = new StudentData(new Integer(params.get("stud")), studentName, term, classes, assignmentBlobs);
        } catch (SiteException e) {
            e.printStackTrace();
        }
        return student;
    }
}
