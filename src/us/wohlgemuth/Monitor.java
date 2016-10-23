package us.wohlgemuth;

import java.util.HashSet;

public class Monitor {
    private Configuration config;

    public Monitor(Configuration config){
        this.config=config;
    }

    public void run(){
        for (Configuration.Site site: config.getSites()){

            System.out.println("Begin site analysis ["+site.getUrl().toExternalForm()+"]");
            SiteParser siteParser = new SiteParser(site.getUrl());
            StudentData currStudentData = siteParser.getStudentData();
            if (null==currStudentData){
                System.err.println("Unable to parse student data");
                continue;
            }
            Integer studentId = currStudentData.getId();

            final String filename = System.getProperty("user.home") + "/.juno/"+studentId.toString()+".ser";
            StudentData prevStudentData = StudentData.deserialize(filename);

            if ((null!=prevStudentData) && (prevStudentData.getTerm().compareTo(currStudentData.getTerm())==0)){
                HashSet<Integer> changeSet = StudentData.getChangeSet(prevStudentData, currStudentData);
                if (changeSet.isEmpty()){
                    System.out.println("No changes detected.");
                    continue;
                }
                System.out.println("Changes detected!");
                for (Integer classId : changeSet) {
                    System.out.println(currStudentData.getClassName(classId));
                    System.out.println(currStudentData.getAssignmentBlob(classId));
                }
                System.out.println("Updating student data.");
            } else {
                System.out.println("Saving initial data for student.");
            }

            currStudentData.serialize(filename);
        }
    }
}
