package com.spyder.pao;

import com.spyder.pao.model.PaoEntry;
import com.spyder.pao.model.EntryType;
import lombok.Data;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;

import static com.spyder.pao.ConsoleColors.*;

public class Quizzer {

    private final Random random;
    private final ConsoleColors console;
    private final DataSource ds;
    private final Scanner scanner = new Scanner(System.in);

    private final String STATE_FILE = "state.properties";

    private int minEntry = 0;
    private int maxEntry = 9;
    private EntryType answerEntryType = EntryType.NUMBER;
    private EntryType givenEntryType = EntryType.PERSON;
    private QuizType quizType = QuizType.GIVEN;

    private final List<EntryType> RANDOM_ENTRY_TYPES = Arrays.asList(EntryType.PERSON, EntryType.ACTION, EntryType.OBJECT);

    public Quizzer(DataSource ds) {
        this.random = new Random();
        this.console = new ConsoleColors();
        this.ds = ds;
    }

    @SneakyThrows
    public void begin() {
        loadState();
        while (true) {
            System.out.println("\n" + currentState());
            System.out.print("> ");
            String line = scanner.nextLine();
            String[] commands = line.split(",");
            for (String command : commands) {
                command = command.trim();
                String[] tokens = command.split(" ");
                if (tokens.length != 0) {
                    String firstToken = tokens[0].toLowerCase();
                    switch (firstToken) {
                        case "help" -> help();
                        case "from" -> setFrom(tokens);
                        case "to" -> setTo(tokens);
                        case "given" -> setGiven(tokens);
                        case "answer" -> setAnswer(tokens);
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
            while((line = br.readLine()) != null) {
                String[] tokens = line.split("=");
                try {
                    PropertyKey key = PropertyKey.valueOf(tokens[0]);
                    switch (key) {
                        case FROM -> minEntry = Integer.parseInt(tokens[1]);
                        case TO -> maxEntry = Integer.parseInt(tokens[1]);
                        case GIVEN -> givenEntryType = EntryType.valueOf(tokens[1]);
                        case ANSWER -> answerEntryType = EntryType.valueOf(tokens[1]);
                        case QUIZ -> quizType = QuizType.valueOf(tokens[1]);
                    }
                } catch(IllegalArgumentException e) {
                    System.out.println("[WARN] No PropertyKey for value " + tokens[0]);
                }
            }
        }
    }

    // Write the quiz state to state.properties so it will be loaded next time the program runs
    @SneakyThrows
    private void writeState() {
        FileWriter myWriter = new FileWriter(stateFileLocation());
        myWriter.write(PropertyKey.FROM.name() + "=" + minEntry + "\n");
        myWriter.write(PropertyKey.TO.name() + "=" + maxEntry + "\n");
        myWriter.write(PropertyKey.GIVEN.name() + "=" + givenEntryType.name() + "\n");
        myWriter.write(PropertyKey.ANSWER.name() + "=" + answerEntryType.name() + "\n");
        myWriter.write(PropertyKey.QUIZ.name() + "=" + quizType.name() + "\n");
        myWriter.close();
    }

    private void setTo(String[] tokens) {
        maxEntry = Integer.parseInt(tokens[1]);
    }

    private void setFrom(String[] tokens) {
        minEntry = Integer.parseInt(tokens[1]);
    }

    private void setGiven(String[] tokens) {
        givenEntryType = EntryType.valueOf(tokens[1].toUpperCase());
    }

    private void setAnswer(String[] tokens) {
        answerEntryType = EntryType.valueOf(tokens[1].toUpperCase());
    }

    private void setQuizType(String[] tokens) {
        String inputQuizType = tokens[1];
        try {
            quizType = QuizType.valueOf(inputQuizType.toUpperCase());
        } catch(IllegalArgumentException e) {
            System.out.println("Quiz type not found - " + inputQuizType);
        }
    }

    private void listEntities() {
        getEntries().forEach(paoEntry -> System.out.println(String.join("  ", paoEntry.getNumberStr(), paoEntry.getPerson(), paoEntry.getAction(), paoEntry.getObject())));
    }

    private void help() {
        System.out.println("\nCommands:");
        System.out.println("  FROM [start number]");
        System.out.println("  TO [end number]");
        System.out.println("  GIVEN [NUMBER | PERSON | ACTION | OBJECT | RANDOM]");
        System.out.println("  ANSWER [NUMBER | PERSON | ACTION | OBJECT | RANDOM]");
        System.out.println("  QUIZ [NUMBER | GIVEN]");
        System.out.println("  BEGIN / START");
        System.out.println("  QUIT / EXIT");
        System.out.println();

    }

    private String currentState() {
        return "quiz " + quizType.name() + ", given " + givenEntryType.name() + ", answer " + answerEntryType.name() + ", " + minEntry + ".." + maxEntry;
    }

    private List<PaoEntry> getEntries() {
        return ds.getEntries().stream()
                .filter(paoEntry -> paoEntry.getNumber() >= minEntry && paoEntry.getNumber() <= maxEntry)
                .collect(Collectors.toList());
    }

    public void quiz() {
        writeState();
        switch (quizType) {
            case NUMBER -> numberQuiz();
            case GIVEN -> givenAnswerQuiz();
        }
    }

    public void numberQuiz() {
        List<PaoEntry> entries = getEntries();
        while (true) {
            PaoEntry person = entries.get(random.nextInt(entries.size()));
            PaoEntry action = entries.get(random.nextInt(entries.size()));
            PaoEntry object = entries.get(random.nextInt(entries.size()));
            String number = person.getNumberStr() + action.getNumberStr() + object.getNumberStr();
            String fullAnswer = person.getPerson() + " " + action.getAction() + " " + object.getObject();
            System.out.println();
            System.out.println("Picture for " + console.color(CYAN, number));
            System.out.print("> ");
            String answerText = scanner.nextLine();
            if ("quit".equalsIgnoreCase(answerText)) {
                return;
            } else if("exit".equalsIgnoreCase(answerText)) {
                System.exit(0);
            }
            System.out.println("Answer: " + console.color(CYAN, fullAnswer));
        }
    }

    public void givenAnswerQuiz() {
        if (maxEntry - minEntry < 1) {
            System.out.println("Invalid min / max");
            return;
        }
        List<PaoEntry> entries = getEntries();

        if (entries.isEmpty()) {
            System.out.println("No entries in range " + minEntry + ".." + maxEntry);
            return;
        }
        Collections.shuffle(entries);
        System.out.println();
        int questionsAskedInCurrentSet = 0;




        while (true) {
            questionsAskedInCurrentSet = questionsAskedInCurrentSet % entries.size();
            if(questionsAskedInCurrentSet == 0) {
                Collections.shuffle(entries);
            }

            EntryType questionGivenEntryType = givenEntryType;
            EntryType questionAnswerEntryType = answerEntryType;
            if(questionGivenEntryType == EntryType.RANDOM) {
                questionGivenEntryType = getRandomEntryType();
            }
            if(questionAnswerEntryType == EntryType.RANDOM) {
                questionAnswerEntryType = getRandomEntryType();
            }

            PaoEntry entry = entries.get(questionsAskedInCurrentSet);
            String t1Word = questionAnswerEntryType.name();
            String t1Value = entry.getValue(questionAnswerEntryType);
            String t2Word = questionGivenEntryType.name();
            String t2Value = entry.getValue(questionGivenEntryType);
            System.out.print(t1Word + " for " + t2Word + " " + console.color(CYAN, t2Value) + ": ");
            String answerText = scanner.nextLine();

            QuestionContext questionContext = new QuestionContext();
            questionContext.setCorrectAnswer(entry.getValue(questionAnswerEntryType));
            questionContext.setEntry(questionGivenEntryType, entry);
            questionContext.setEntry(questionAnswerEntryType, entry);
            questionContext.setUserAnswerText(answerText);
            validateAnswer(questionContext, questionAnswerEntryType);

            if ("quit".equalsIgnoreCase(answerText)) {
                return;
            } else if("exit".equalsIgnoreCase(answerText)) {
                System.exit(0);
            } else if (questionContext.isCorrect()) {
                String extraText = questionContext.isExactlyCorrect() ? "" : (" " + console.color(CYAN, questionContext.getCorrectAnswer()));
                System.out.println(console.color(GREEN, "Correct" + extraText + "\n"));
            } else {
                System.out.println(console.color(RED, "Wrong:   " + t2Value + " has " + t1Word + " ") + console.color(CYAN, t1Value) + "\n");
            }
            questionsAskedInCurrentSet++;
        }
    }

    private EntryType getRandomEntryType() {
        return RANDOM_ENTRY_TYPES.get(random.nextInt(RANDOM_ENTRY_TYPES.size()));
    }

    private void validateAnswer(QuestionContext questionContext, EntryType answerType) {
        List<String> answerList = questionContext.getEntry(answerType).getAllByType(answerType);
        int bestCorrectWords = -1;
        int wrongWords = 0;
        String[] userAnswerTokens = questionContext.getUserAnswerText().split(" ");
        List<String> bestMatchedAnswer = Collections.emptyList();

        for(String possibleAnswer : answerList) {
            List<String> possibleAnswerTokens = Arrays.asList(possibleAnswer.split(" "));
            int correctWords = 0;
            for (String userAnswerToken : userAnswerTokens) {
                correctWords += possibleAnswerTokens.contains(userAnswerToken.toLowerCase()) ? 1 : 0;
            }
            int newWrongWords = Math.max(possibleAnswerTokens.size(), userAnswerTokens.length) - correctWords;
            if(correctWords > bestCorrectWords || (correctWords == bestCorrectWords && newWrongWords < wrongWords)) {
                bestCorrectWords = correctWords;
                wrongWords = Math.max(possibleAnswerTokens.size(), userAnswerTokens.length) - correctWords;
                bestMatchedAnswer = possibleAnswerTokens;
            }
        }
        // Allow one word wrong if the answer is longer, probably got the gist of it
        int allowedWordsWrong = bestMatchedAnswer.size() > 2 ? 1 : 0;

        questionContext.setCorrect(wrongWords <= allowedWordsWrong);
        questionContext.setExactlyCorrect(questionContext.getUserAnswerText().equalsIgnoreCase(answerList.get(0)));
    }

    private enum PropertyKey {
        FROM, TO, GIVEN, ANSWER, QUIZ
    }

    private enum QuizType {
        GIVEN, NUMBER
    }

    @Data
    private static class QuestionContext {
        private QuizType quizType;
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

}
