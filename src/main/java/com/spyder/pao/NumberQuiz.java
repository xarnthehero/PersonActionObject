package com.spyder.pao;

import com.spyder.pao.model.PaoEntry;
import com.spyder.pao.model.QuizConfiguration;

import java.util.List;
import java.util.Random;
import java.util.Scanner;

import static com.spyder.pao.ConsoleColors.CYAN;
import static com.spyder.pao.ConsoleColors.color;

public class NumberQuiz {

    private final Random random;
    private final Scanner stdInScanner;

    public NumberQuiz(Scanner stdInScanner) {
        random = new Random();
        this.stdInScanner = stdInScanner;
    }

    public void beginQuiz(QuizConfiguration config, DataSource ds) {
        List<PaoEntry> entries = ds.getEntries(config);
        while (true) {
            PaoEntry person = entries.get(random.nextInt(entries.size()));
            PaoEntry action = entries.get(random.nextInt(entries.size()));
            PaoEntry object = entries.get(random.nextInt(entries.size()));
            String number = person.getNumberStr() + action.getNumberStr() + object.getNumberStr();
            String fullAnswer = person.getPerson() + " " + action.getAction() + " " + object.getObject();
            System.out.println();
            System.out.println("Picture for " + color(CYAN, number));
            System.out.print("> ");
            String answerText = stdInScanner.nextLine();
            if ("quit".equalsIgnoreCase(answerText)) {
                return;
            } else if ("exit".equalsIgnoreCase(answerText)) {
                System.exit(0);
            }
            System.out.println("Answer: " + color(CYAN, fullAnswer));
        }
    }
}
