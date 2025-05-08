package com.spyder.pao.model;

import lombok.Data;

@Data
public class QuestionContext {
    private PaoEntry number;
    private PaoEntry person;
    private PaoEntry action;
    private PaoEntry object;
    // randomXxxxxType is set on a question by question basis and is used temporarily to know what type we are looking at right now.
    private EntryType randomQuestionType;
    private EntryType randomAnswerType;
    private String userAnswerText;
    private boolean correct;
    private boolean exactlyCorrect;
    private String correctAnswer;

    public void setEntry(EntryType entryType, PaoEntry entity) {
        switch (entryType) {
            case NUMBER -> number = entity;
            case PERSON -> person = entity;
            case ACTION -> action = entity;
            case OBJECT -> object = entity;
        }
    }

    public PaoEntry getEntry(EntryType entryType) {
        return switch (entryType) {
            case NUMBER -> number;
            case PERSON -> person;
            case ACTION -> action;
            case OBJECT -> object;
            default -> throw new IllegalArgumentException("Invalid type: " + entryType);
        };
    }
}