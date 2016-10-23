package us.wohlgemuth;

import java.util.HashSet;

public class Monitor {
    private Configuration config;

    public Monitor(Configuration config) {
        this.config = config;
    }

    public void run() {

        Notifier notifier = new Notifier(config.getSmtpHost(), config.getSmtpUser(), config.getSmtpPassword());

        while (true) {
            for (Configuration.Site site : config.getSites()) {

                System.out.println("Begin site analysis [" + site.getUrl().toExternalForm() + "]");
                SiteParser siteParser = new SiteParser(site.getUrl());
                StudentData currStudentData = siteParser.getStudentData();
                if (null == currStudentData) {
                    System.err.println("Unable to parse student data");
                    continue;
                }
                Integer studentId = currStudentData.getId();
                String filename = System.getProperty("user.home") + "/.juno/" + studentId.toString() + ".ser";
                StudentData prevStudentData = StudentData.deserialize(filename);
                //prevStudentData = new StudentData(currStudentData.getId(),currStudentData.getName(), currStudentData.getTerm(), null, null);

                if ((null != prevStudentData) && (prevStudentData.getTerm().compareTo(currStudentData.getTerm()) == 0)) {
                    HashSet<Integer> changeSet = StudentData.getChangeSet(prevStudentData, currStudentData);
                    if (changeSet.isEmpty()) {
                        System.out.println("No changes detected.");
                        continue;
                    }
                    System.out.println("Changes detected!");
                    StringBuilder body = new StringBuilder();
                    for (Integer classId : changeSet) {
                        body.append(currStudentData.getClassName(classId));
                        body.append("\n\n");
                        body.append(currStudentData.getAssignmentBlob(classId));
                        body.append("\n");
                    }
                    System.out.println(body.toString());
                    String subject = "Jupiter Change Detected ("+currStudentData.getTerm()+") for " + currStudentData.getName();
                    notifier.sendEmail(subject, body.toString(), site.getemailAddresses());
                    System.out.println("Updating student data.");
                } else {
                    System.out.println("Saving initial data for student.");
                }

                currStudentData.serialize(filename);
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
