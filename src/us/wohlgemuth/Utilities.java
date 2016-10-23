package us.wohlgemuth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

public class Utilities {

    private static ArrayList<String> getMissingString(ArrayList<String> list1, ArrayList<String> list2) {
        ArrayList<String> missingList = new ArrayList<>();
        for (String s1 : list1) {
            boolean found = false;
            for (String s2 : list2) {
                if (s1.compareTo(s2) == 0) {
                    found = true;
                    break;
                }
            }
            if (!found) missingList.add(s1);
        }
        return missingList;
    }

    private static ArrayList<String> getArrayOfLines(String s) {
        ArrayList<String> list = new ArrayList<>();
        if (s == null) return list;
        StringReader stringReader = new StringReader(s);
        BufferedReader bufferedReader = new BufferedReader(stringReader);
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                list.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    private static String annotateListToString(String prefix, ArrayList<String> list) {
        StringBuilder diff = new StringBuilder();
        for (String s : list) {
            diff.append(prefix);
            diff.append(s);
            diff.append("\n");
        }
        return diff.toString();
    }

    public static String Diff(String a, String b) {
        StringBuilder diff = new StringBuilder();
        ArrayList<String> aList = getArrayOfLines(a);
        ArrayList<String> bList = getArrayOfLines(b);
        ArrayList<String> removedList = getMissingString(aList, bList);
        ArrayList<String> addedList = getMissingString(bList, aList);
        diff.append(annotateListToString("[-] ", removedList));
        diff.append(annotateListToString("[+] ", addedList));
        return diff.toString();
    }
}
