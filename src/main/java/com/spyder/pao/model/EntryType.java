package com.spyder.pao.model;

public enum EntryType {
    NUMBER,
    PERSON,
    ACTION,
    OBJECT,
    RANDOM
    ;

    public static EntryType valueOfNullable(String value) {
        try {
            return EntryType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
