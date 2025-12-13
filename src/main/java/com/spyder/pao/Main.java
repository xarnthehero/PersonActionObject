package com.spyder.pao;

public class Main {
    public static void main(String[] args) {
        DataSource dataSource = DataSource.createAndLoad();
        CLIRunner CLIRunner = new CLIRunner(dataSource);
        CLIRunner.begin();
    }
}