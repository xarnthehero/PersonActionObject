package com.spyder.pao;

public class Main {
    public static void main(String[] args) {
        DataSource dataSource = DataSource.createAndLoad();
        dataSource.writeEquallySpaced();
        Quizzer quizzer = new Quizzer(dataSource);
        quizzer.begin();
    }
}