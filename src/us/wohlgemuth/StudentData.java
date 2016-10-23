package us.wohlgemuth;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;

public class StudentData implements Serializable{
    private Integer id;
    private String name;
    private String term;
    HashMap<Integer, String> classes;
    HashMap<Integer, String> assignmentBlobs;

    public StudentData(Integer id, String name, String term, HashMap<Integer, String> classes, HashMap<Integer, String> assignmentBlobs){
        this.id=id;
        this.name=name;
        this.term=term;
        this.classes=classes;
        this.assignmentBlobs=assignmentBlobs;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTerm() {
        return term;
    }

    public String getClassName(Integer classId){
        if (null==classes) return null;
        if (null==classId) return null;
        if (classes.containsKey(classId)){
            return classes.get(classId);
        }
        return null;
    }

    private HashMap<Integer, Integer> getAssignmentHashCodes(){
        HashMap<Integer, Integer> hashCodes = new HashMap<>();
        if (null!=assignmentBlobs){
            for (Integer id: assignmentBlobs.keySet()){
                hashCodes.put(id, assignmentBlobs.get(id).hashCode());
            }
        }
        return hashCodes;
    }

    public String getAssignmentBlob(Integer classId){
        if (null==classes) return null;
        if (null==classId) return null;
        if (assignmentBlobs.containsKey(classId)){
            return assignmentBlobs.get(classId);
        }
        return null;
    }

    public static HashSet<Integer> getChangeSet(StudentData studentData1, StudentData studentData2){
        HashSet<Integer> changeSet = new HashSet<>();
        HashMap<Integer, Integer> hashCodes1;
        HashMap<Integer, Integer> hashCodes2;
        if (null!=studentData1){
            hashCodes1=studentData1.getAssignmentHashCodes();
        }else{
            hashCodes1=new HashMap<>();
        }
        if (null!=studentData2){
            hashCodes2=studentData2.getAssignmentHashCodes();
        }else{
            hashCodes2=new HashMap<>();
        }
        for (Integer classId: hashCodes1.keySet()){
            if (changeSet.contains(classId)) continue;
            if (hashCodes2.containsKey(classId)){
                if (hashCodes1.get(classId).intValue()!=hashCodes2.get(classId).intValue()){
                    changeSet.add(classId);
                }
            }else{
                changeSet.add(classId);
            }
        }
        for (Integer classId: hashCodes2.keySet()){
            if (changeSet.contains(classId)) continue;
            if (hashCodes1.containsKey(classId)){
                if (hashCodes1.get(classId).intValue()!=hashCodes2.get(classId).intValue()){
                    changeSet.add(classId);
                }
            }else{
                changeSet.add(classId);
            }
        }
        return changeSet;
    }

    public static StudentData deserialize(String filename){
        StudentData studentData;
        try {
            FileInputStream fileInputStream = new FileInputStream(filename);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            studentData = (StudentData) objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();
        }catch (ClassNotFoundException|IOException e){
            return null;
        }
        return studentData;
    }

    public boolean serialize(String filename){
        try {
            File file = new File(filename);
            if (!file.exists()){
                File dirs = new File(file.getParent());
                dirs.mkdirs();
                file.createNewFile();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(this);
            objectOutputStream.close();
            fileOutputStream.close();
        }catch(IOException e) {
            return false;
        }
        return true;
    }
}
