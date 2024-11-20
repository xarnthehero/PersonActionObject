package com.spyder.pao.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class PaoEntry {

    private String numberStr;
    private String person;
    private String action;
    private String object;
    private String altNumbersStr;
    private String altPeopleStr;
    private String altActionsStr;
    private String altObjectsStr;

    // Calculated
    private List<String> allNumbersList;
    private List<String> allPeopleList;
    private List<String> allActionsList;
    private List<String> allObjectsList;

    private static final String ALT_DELIMITER = ",";

    public int getNumber() {
        return Integer.parseInt(numberStr);
    }

    public String getValue(Type type) {
        return switch (type) {
            case NUMBER -> numberStr;
            case PERSON -> person;
            case ACTION -> action;
            case OBJECT -> object;
        };
    }

    public List<String> getAllByType(Type type) {
        return switch (type) {
            case NUMBER -> getAllNumbers();
            case PERSON -> getAllPeopleList();
            case ACTION -> getAllActions();
            case OBJECT -> getAllObjects();
        };
    }

    public List<String> getAllNumbers() {
        if(allNumbersList == null) {
            allNumbersList = createList(numberStr, altNumbersStr);
        }
        return allNumbersList;
    }

    public List<String> getAllPeopleList() {
        if(allPeopleList == null) {
            allPeopleList = createList(person, altPeopleStr);
        }
        return allPeopleList;
    }

    public List<String> getAllActions() {
        if(allActionsList == null) {
            allActionsList = createList(action, altActionsStr);
        }
        return allActionsList;
    }

    public List<String> getAllObjects() {
        if(allObjectsList == null) {
            allObjectsList = createList(object, altObjectsStr);
        }
        return allObjectsList;
    }

    private List<String> createList(String str, String altStr) {
        List<String> strList = new ArrayList<>();
        strList.add(str);
        strList.addAll(Arrays.asList(altStr.split(ALT_DELIMITER)));
        strList.removeIf(s -> s == null || s.isEmpty());
        return strList;
    }
}