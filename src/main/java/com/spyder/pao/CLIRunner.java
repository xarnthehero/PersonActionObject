package com.spyder.pao;

import com.spyder.pao.model.EntryType;
import com.spyder.pao.model.QuizConfiguration;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

import static com.spyder.pao.ConsoleColors.*;

public class CLIRunner {

    private final DataSource ds;
    private final Scanner stdInScanner = new Scanner(System.in);
    private final GivenQuiz givenQuiz;
    private final NumberQuiz numberQuiz;

    private final String STATE_FILE = "state.properties";

    private final QuizConfiguration quizConfiguration;
    private QuizType quizType = QuizType.GIVEN;

    public CLIRunner(DataSource ds) {
        this.ds = ds;
        quizConfiguration = new QuizConfiguration();
        givenQuiz = new GivenQuiz(stdInScanner);
        numberQuiz = new NumberQuiz(stdInScanner);
    }

    @SneakyThrows
    public void begin() {
        loadState();
        while (true) {
            System.out.println(System.lineSeparator() + currentState());
            System.out.print("> ");
            String line = stdInScanner.nextLine();
            String[] commands = line.split(",");
            for (String command : commands) {
                command = command.trim();
                String[] tokens = command.split(" ");
                if (tokens.length != 0) {
                    String firstToken = tokens[0].toLowerCase();
                    switch (firstToken) {
                        case "help" -> help();
                        case "from" -> quizConfiguration.setMinEntry(Integer.parseInt(tokens[1]));
                        case "to" -> quizConfiguration.setMaxEntry(Integer.parseInt(tokens[1]));
                        case "given" -> quizConfiguration.setGivenEntryType(EntryType.valueOf(tokens[1].toUpperCase()));
                        case "answer" -> quizConfiguration.setAnswerEntryType(EntryType.valueOf(tokens[1].toUpperCase()));
                        case "quiz" -> setQuizType(tokens);
                        case "list" -> listEntities();
                        case "begin", "b", "start" -> quiz();
                        case "quit", "exit" -> System.exit(0);
                    }
                }
            }
        }

    }

    private String stateFileLocation() {
        return DataSource.RESOURCES_DIR + "/" + STATE_FILE;
    }

    @SneakyThrows
    private void loadState() {
        try (BufferedReader br = new BufferedReader(new FileReader(stateFileLocation()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("=");
                try {
                    PropertyKey key = PropertyKey.valueOf(tokens[0]);
                    switch (key) {
                        case FROM -> quizConfiguration.setMinEntry(Integer.parseInt(tokens[1]));
                        case TO -> quizConfiguration.setMaxEntry(Integer.parseInt(tokens[1]));
                        case GIVEN -> quizConfiguration.setGivenEntryType(EntryType.valueOf(tokens[1]));
                        case ANSWER -> quizConfiguration.setAnswerEntryType(EntryType.valueOf(tokens[1]));
                        case QUIZ -> quizType = QuizType.valueOf(tokens[1]);
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("[WARN] No PropertyKey for value " + tokens[0]);
                }
            }
        }
    }

    // Write the quiz state to state.properties so it will be loaded next time the program runs
    @SneakyThrows
    private void writeState() {
        FileWriter myWriter = new FileWriter(stateFileLocation());
        String configOutput = String.join(
                System.lineSeparator(),
                PropertyKey.FROM.name() + "=" + quizConfiguration.getMinEntry(),
                PropertyKey.TO.name() + "=" + quizConfiguration.getMaxEntry(),
                PropertyKey.GIVEN.name() + "=" + quizConfiguration.getGivenEntryType(),
                PropertyKey.ANSWER.name() + "=" + quizConfiguration.getAnswerEntryType(),
                PropertyKey.QUIZ.name() + "=" + quizType.name()
        );
        myWriter.write(configOutput + System.lineSeparator());
        myWriter.close();
    }

    private void setQuizType(String[] tokens) {
        String inputQuizType = tokens[1];
        try {
            quizType = QuizType.valueOf(inputQuizType.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("Quiz type not found - " + inputQuizType);
        }
    }

    private void listEntities() {
        ds.getEntries(quizConfiguration).forEach(paoEntry -> System.out.println(
                        String.join(" | ",
                                paoEntry.getNumberStr(),
                                color(CYAN, paoEntry.getPerson()),
                                color(GREEN, paoEntry.getAction()),
                                color(PURPLE, paoEntry.getObject()))
                )
        );
    }

    private void help() {
        System.out.println(System.lineSeparator());
        System.out.println(
                """
                        Commands:
                          \tFROM [start number]
                          \tTO [end number]
                          \tGIVEN [NUMBER | PERSON | ACTION | OBJECT | RANDOM]
                          \tANSWER [NUMBER | PERSON | ACTION | OBJECT | RANDOM]
                          \tQUIZ [NUMBER | GIVEN]
                          \tBEGIN / START
                          \tQUIT / EXIT
                """
        );
    }

    private String currentState() {
        return "quiz " + quizType.name()
                + ", given " + quizConfiguration.getGivenEntryType().name() 
                + ", answer " + quizConfiguration.getAnswerEntryType().name()
                + ", " + quizConfiguration.getMinEntry() + ".." + quizConfiguration.getMaxEntry();
    }

    public void quiz() {
        writeState();
        switch (quizType) {
            case NUMBER -> numberQuiz.beginQuiz(quizConfiguration, ds);
            case GIVEN -> givenQuiz.beginQuiz(quizConfiguration, ds);
        }
    }



    private enum PropertyKey {
        FROM, TO, GIVEN, ANSWER, QUIZ
    }

    private enum QuizType {
        GIVEN, NUMBER
    }

}
