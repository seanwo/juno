package us.wohlgemuth;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SiteParser {

    public class SiteException extends Exception {
        public SiteException(String msg) {
            super(msg);
        }
    }

    private URL url;
    private String cookie;

    private final String ClearanceCookieName = "cf_clearance";
    private final String UserAgentString = "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36";

    public SiteParser(URL url, String cookie) {
        this.url = url;
        this.cookie = cookie;
    }

    private Connection connect(URL url, Connection.Method method) {
        return Jsoup.connect(url.toExternalForm())
                .userAgent(UserAgentString)
                .ignoreHttpErrors(true)
                .method(method);
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

    private Map<String, String> getCookies(Connection.Response response, ArrayList<String> names) throws SiteException {
        HashMap<String, String> cookies = new HashMap<>();

        for (Iterator<String> i = names.iterator(); i.hasNext(); ) {
            String name = i.next();
            String cookie = response.cookie(name);
            if ((null != cookie) || (!cookie.isEmpty())) {
                cookies.put(name, cookie);
            }
        }

        if (cookies.size() != names.size()) {
            throw new SiteException("unable to acquire all required cookies");
        }

        return cookies;
    }

    private void setCookies(Connection connection, Map<String, String> cookies) {
        if (cookies.size() > 0) {
            connection.cookies(cookies);
        }
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

    private Document navigate(URL url, Map<String, String> cookies, HashMap<String, String> params) throws SiteException {
        Connection connection = connect(url, Connection.Method.POST);
        setCookies(connection, cookies);
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

    private HashMap<String, String> getFormParams(Document document) throws SiteException {
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
        return params;
    }

    private void setFormParams(Connection connection, HashMap<String, String> params) {
        connection.data(params);
    }

    private HashMap<Integer, String> getClasses(Document document) throws SiteException {
        Elements elements = document.getElementsByAttributeValueStarting("click", "postval(\'class1\'");

        if (elements.size() <= 0) {
            throw new SiteException("unable to determine classes from clickable tabs");
        }

        HashMap<Integer, String> classes = new HashMap<>();

        for (Element e: elements){

            String attrClick = e.attr("click");

            Pattern menuPattern = Pattern.compile("'class1',(.*?)\\)");
            Matcher menuMatcher = menuPattern.matcher(attrClick);

            if (!menuMatcher.find()) {
                throw new SiteException("unable to determine a class id");
            }

            String classid = menuMatcher.group(1);

            Elements classnavElements = e.getElementsByClass("classnav");

            if (classnavElements.size() <= 0) {
                throw new SiteException("unable to determine class name from clickable tab");
            }

            String classname = classnavElements.get(0).text().trim();

            classes.put(new Integer(classid), classname);
        }

        return classes;
    }

    private String getTermName(Document document) throws SiteException {
        Elements elements = document.getElementsByTag("script");
        if (elements.size() <= 0) {
            throw new SiteException("unable to determine terms menu script");
        }
        Element element = null;
        for (Element e : elements) {
            if (e.data().contains("menu(\'datemenu1\'")) {
                element = e;
                break;
            }
        }
        if (element == null) {
            throw new SiteException("unable to determine terms menu script");
        }

        String menu = element.data();

        Pattern termIndexPattern = Pattern.compile("\\\"(.*?)\\\"");
        Matcher termIndexMatcher = termIndexPattern.matcher(menu);
        if (!termIndexMatcher.find()) {
            throw new SiteException("unable to determine term index");
        }
        String index = termIndexMatcher.group(1);

        Pattern menuPattern = Pattern.compile("\\[(.*?)\\]");
        Matcher menuMatcher = menuPattern.matcher(menu);

        HashMap<String, String> terms = new HashMap<>();

        while (menuMatcher.find()) {
            String nameValuePair = menuMatcher.group(1);
            Pattern nameValuePairPattern = Pattern.compile("\\\"(.*?)\\\"");
            Matcher nameValuePairMatcher = nameValuePairPattern.matcher(nameValuePair);

            if (!nameValuePairMatcher.find()) {
                throw new SiteException("unable to determine terms");
            }
            String name = nameValuePairMatcher.group(1);
            if (!nameValuePairMatcher.find()) {
                throw new SiteException("unable to determine terms");
            }
            String value = nameValuePairMatcher.group(1);

            terms.put(name, value);
        }

        if (terms.isEmpty()) {
            throw new SiteException("unable to determine terms");
        }

        return terms.get(index);
    }

    private String getStudentName(Document document) throws SiteException {
        Elements elements = document.getElementsByAttributeValue("id", "studtab");
        if (elements.size() != 1) {
            throw new SiteException("unable to determine current student");
        }
        Element element = elements.get(0);
        String studentName = element.text();
        studentName = studentName.replaceAll("Parent of ", "");
        return studentName;
    }

    private String getAssignments(Document document) {
        StringBuilder assignmentBuilder = new StringBuilder();

        Elements eAssignments = document.getElementsByAttributeValueStarting("click", "goassign");
        for (Element eAssignment : eAssignments) {
            Elements items = eAssignment.getAllElements();
            if (items.size() != 0) {
                Element item = items.first();
                assignmentBuilder.append(item.text());
                assignmentBuilder.append("\n");
            }
        }
        return assignmentBuilder.toString();
    }

    private HashMap<Integer, String> getAssignmentBlobs(Set<Integer> classes, URL url, Map<String, String> cookies, HashMap<String, String> params) throws SiteException {
        HashMap<Integer, String> assignmentBlobs = new HashMap<>();
        Iterator<Integer> it = classes.iterator();
        while (it.hasNext()) {
            Integer classId = it.next();
            params.put("to", "grades");
            params.put("class1", classId.toString());
            Document document = navigate(url, cookies, params);
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
            Map<String, String> cookies;
            ArrayList<String> cookieNames = new ArrayList<>();
            if (response.body().contains("jschl-answer")) {
                response = solveChallenge(response);
                cookieNames.add(ClearanceCookieName);
            }
            cookies = getCookies(response, cookieNames);
            cookies.put("first", cookie);
            Document document = getDocument(response);
            URL formUrl = getFormURL(document);
            HashMap<String, String> params = getFormParams(document);
            document = navigate(formUrl, cookies, params);
            formUrl = getFormURL(document);
            params = getFormParams(document);
            params.put("to", "todo");
            document = navigate(formUrl, cookies, params);
            String studentName = getStudentName(document);
            String termName = getTermName(document);
            HashMap<Integer, String> classes = getClasses(document);
            HashMap<Integer, String> assignmentBlobs = getAssignmentBlobs(classes.keySet(), formUrl, cookies, params);
            student = new StudentData(new Integer(params.get("stud")), studentName, termName, classes, assignmentBlobs);
        } catch (SiteException e) {
            e.printStackTrace();
        }
        return student;
    }

    public Connection.Response solveChallenge(Connection.Response challengeResponse) throws SiteException {
        String body = challengeResponse.body();
        URL url = challengeResponse.url();
        String host = url.getHost();
        Document doc = getDocument(challengeResponse);
        URL challengeUrl = getFormURL(doc);

        Pattern pattern = Pattern.compile("name=\"s\" value=\"(.+?)\"");
        Matcher matcher = pattern.matcher(body);
        if (!matcher.find()) {
            throw new SiteException("Unable to find challenge (s)");
        }
        String s = matcher.group(1);

        pattern = Pattern.compile("name=\"jschl_vc\" value=\"(\\w+)\"");
        matcher = pattern.matcher(body);
        if (!matcher.find()) {
            throw new SiteException("Unable to find challenge (jschl_vc)");
        }
        String jsChlVc = matcher.group(1);

        pattern = Pattern.compile("name=\"pass\" value=\"(.+?)\"");
        matcher = pattern.matcher(body);
        if (!matcher.find()) {
            throw new SiteException("Unable to find challenge (pass)");
        }
        String pass = matcher.group(1);

        pattern = Pattern.compile("f.submit\\(\\);\\r?\\n.+\\},.([0-9]*)");
        matcher = pattern.matcher(body);
        if (!matcher.find()) {
            throw new SiteException("Unable to find challenge (timeout)");
        }
        String timeoutvalue = matcher.group(1);
        int timeout = Integer.parseInt(timeoutvalue);

        pattern = Pattern.compile("getElementById\\('cf-content'\\)[\\s\\S]+?setTimeout.+?\\r?\\n([\\s\\S]+?a\\.value =.+?)\\r?\\n", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(body);
        if (!matcher.find()) {
            throw new SiteException("Unable to find challenge (method)");
        }
        String challenge = matcher.group(1);

        pattern = Pattern.compile("\\s{3,}[a-z](?: = |\\.).+", Pattern.MULTILINE);
        challenge = pattern.matcher(challenge).replaceAll("");
        pattern = Pattern.compile("a\\.value = .+", Pattern.MULTILINE);
        challenge = pattern.matcher(challenge).replaceAll("");

        ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
        Double answer;
        try {
            answer = (Double) engine.eval(challenge) + host.length();
        } catch (ScriptException e) {
            throw new SiteException("Unable to evaluate challenge method");
        }
        String formatedAnswer = String.format("%.10f", answer);
        formatedAnswer = formatedAnswer.indexOf(".") < 0 ? formatedAnswer : formatedAnswer.replaceAll("0*$", "").replaceAll("\\.$", "");

        HashMap<String, String> params = new HashMap<>();
        params.put("s", s);
        params.put("jschl_vc", jsChlVc);
        params.put("pass", pass);
        params.put("jschl_answer", formatedAnswer);

        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
        }

        Connection connection = Jsoup.connect(challengeUrl.toExternalForm())
                .userAgent(UserAgentString)
                .referrer(url.toString())
                .data(params)
                .method(Connection.Method.GET)
                .followRedirects(true);
        Connection.Response response = getResponse(connection);

        if (!response.cookies().containsKey(ClearanceCookieName)) {
            throw new SiteException("Unable to obtain clearance cookie");
        }

        return response;
    }
}
