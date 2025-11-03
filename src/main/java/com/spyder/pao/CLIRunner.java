package com.spyder.pao;

import com.spyder.pao.model.Command;
import com.spyder.pao.model.EntryType;
import com.spyder.pao.model.QuizConfiguration;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.function.Consumer;

import static com.spyder.pao.ConsoleColors.*;

public class CLIRunner {

    private final DataSource ds;
    private final Scanner stdInScanner = new Scanner(System.in);
    private final GivenQuiz givenQuiz;
    private final NumberQuiz numberQuiz;

    private final String STATE_FILE = "state.properties";

    private final QuizConfiguration quizConfiguration;
    private QuizType quizType = QuizType.GIVEN;
    private int timerMinutes = 0; // 0 means timer is off

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
            String[] commandTokens = line.split(",");
            for (String commandToken : commandTokens) {
                commandToken = commandToken.trim();
                String[] tokens = commandToken.split(" ");
                if (tokens.length != 0) {
                    String firstToken = tokens[0].toLowerCase();
                    Command command = Command.getCommand(firstToken);
                    switch (command) {
                        case HELP -> help();
                        case FROM -> quizConfiguration.setMinEntry(Integer.parseInt(tokens[1]));
                        case TO -> quizConfiguration.setMaxEntry(Integer.parseInt(tokens[1]));
                        case GIVEN, ANSWER -> {
                            Consumer<EntryType> setter = Command.GIVEN == command ? quizConfiguration::setGivenEntryType : quizConfiguration::setAnswerEntryType;
                            Optional<EntryType> type = Optional.ofNullable(EntryType.valueOfNullable(tokens[1].toUpperCase()));
                            type.ifPresentOrElse(
                                    setter,
                                    () -> System.out.printf("Invalid %s type", command)
                            );
                        }
                        case QUIZ_TYPE -> setQuizType(tokens);
                        case TIMER -> setTimer(tokens);
                        case LIST -> listEntities();
                        case BEGIN_QUIZ -> quiz();
                        case QUIT, EXIT -> System.exit(0);
                        case null -> System.out.println(color(RED, "Unrecognized command"));
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
                        case TIMER -> timerMinutes = Integer.parseInt(tokens[1]);
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
                PropertyKey.QUIZ.name() + "=" + quizType.name(),
                PropertyKey.TIMER.name() + "=" + timerMinutes
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

    private void setTimer(String[] tokens) {
        String timerValue = tokens[1].toLowerCase();
        if ("off".equals(timerValue)) {
            timerMinutes = 0;
            System.out.println("Timer disabled");
        } else {
            try {
                timerMinutes = Integer.parseInt(timerValue);
                if (timerMinutes < 0) {
                    System.out.println("Timer must be a positive number or 'off'");
                    timerMinutes = 0;
                } else {
                    System.out.println("Timer set to " + timerMinutes + " minute" + (timerMinutes == 1 ? "" : "s"));
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid timer value - use a number or 'off'");
            }
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
  FROM [start number]
  TO [end number]
  GIVEN [NUMBER | PERSON | ACTION | OBJECT | RANDOM]
  ANSWER [NUMBER | PERSON | ACTION | OBJECT | RANDOM]
  QUIZ [NUMBER | GIVEN]
  TIMER [minutes | off]
  BEGIN / START
  QUIT / EXIT
                """
        );
    }

    private String currentState() {
        String timerStr = timerMinutes > 0 ? timerMinutes + " min" : "off";
        return "quiz " + quizType.name()
                + ", given " + quizConfiguration.getGivenEntryType().name()
                + ", answer " + quizConfiguration.getAnswerEntryType().name()
                + ", " + quizConfiguration.getMinEntry() + ".." + quizConfiguration.getMaxEntry()
                + ", timer " + timerStr;
    }

    public void quiz() {
        writeState();
        switch (quizType) {
            case NUMBER -> numberQuiz.beginQuiz(quizConfiguration, ds, timerMinutes);
            case GIVEN -> givenQuiz.beginQuiz(quizConfiguration, ds, timerMinutes);
        }
    }



    private enum PropertyKey {
        FROM, TO, GIVEN, ANSWER, QUIZ, TIMER
    }

    private enum QuizType {
        GIVEN, NUMBER
    }

}
