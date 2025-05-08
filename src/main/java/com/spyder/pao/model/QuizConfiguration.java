package com.spyder.pao.model;

import lombok.Data;

@Data
public class QuizConfiguration {

    private int minEntry = 0;
    private int maxEntry = 9;
    private EntryType answerEntryType = EntryType.NUMBER;
    private EntryType givenEntryType = EntryType.PERSON;
}
