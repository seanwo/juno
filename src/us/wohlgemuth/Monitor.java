package us.wohlgemuth;

import java.net.URL;
import java.util.HashSet;

public class Monitor {
    private Configuration config;
    Notifier notifier;

    public Monitor(Configuration config) {
        this.config = config;
        notifier = new Notifier(config.getSmtpHost(), config.getSmtpUser(), config.getSmtpPassword());
    }

    private StudentData getCurrentStudentData(URL url) {
        SiteParser siteParser = new SiteParser(url);
        return siteParser.getStudentData();
    }

    private StudentData getPreviousStudentData(Integer studentId) {
        String filename = System.getProperty("user.home") + "/.juno/" + studentId.toString() + ".ser";
        return StudentData.deserialize(filename);
    }

    private void serializeStudentData(StudentData studentData) {
        String filename = System.getProperty("user.home") + "/.juno/" + studentData.getId() + ".ser";
        studentData.serialize(filename);
    }

    private void sendNotification(HashSet<Integer> changeSet, StudentData prevStudentData, StudentData currStudentData, Configuration.Site site) {
        StringBuilder body = new StringBuilder();
        for (Integer classId : changeSet) {
            body.append(currStudentData.getClassName(classId));
            body.append("\n\n");
            String diffs = Utilities.Diff(prevStudentData.getAssignmentBlob(classId), currStudentData.getAssignmentBlob(classId));
            body.append(diffs);
            body.append("\n");
        }
        System.out.println(body.toString());
        String subject = "Jupiter Change Detected (" + currStudentData.getTerm() + ") for " + currStudentData.getName();
        notifier.sendEmail(subject, body.toString(), site.getemailAddresses());
    }

    private boolean detectChanges(StudentData prevStudentData, StudentData currStudentData, Configuration.Site site) {
        HashSet<Integer> changeSet = StudentData.getChangeSet(prevStudentData, currStudentData);
        if (changeSet.isEmpty()) {
            System.out.println("No changes detected.");
            return false;
        }
        System.out.println("Changes detected!");
        sendNotification(changeSet, prevStudentData, currStudentData, site);
        return true;
    }

    public void run() {
        while (true) {
            for (Configuration.Site site : config.getSites()) {
                System.out.println("Begin site analysis [" + site.getUrl().toExternalForm() + "]");
                StudentData currStudentData = getCurrentStudentData(site.getUrl());
                if (null == currStudentData) {
                    System.err.println("Unable to parse student data");
                    continue;
                }
                Integer studentId = currStudentData.getId();
                StudentData prevStudentData = getPreviousStudentData(studentId);
                //prevStudentData = new StudentData(currStudentData.getId(), currStudentData.getName(), currStudentData.getTerm(), null, null);
                if ((null != prevStudentData) && (prevStudentData.getTerm().compareTo(currStudentData.getTerm()) == 0)) {
                    if (!detectChanges(prevStudentData, currStudentData, site)) {
                        continue;
                    }
                    System.out.println("Updating student data.");
                } else {
                    System.out.println("Saving initial data for student.");
                }
                serializeStudentData(currStudentData);
            }
            if (config.getIntervalMinutes() == 0) break;
            try {
                Thread.sleep(config.getIntervalMinutes() * 60 * 1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
