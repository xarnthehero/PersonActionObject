package com.spyder.pao;

import com.spyder.pao.model.*;

import java.util.*;

import static com.spyder.pao.ConsoleColors.*;

public class GivenQuiz {

    private final List<EntryType> RANDOM_ENTRY_TYPES = Arrays.asList(EntryType.PERSON, EntryType.ACTION, EntryType.OBJECT, EntryType.NUMBER);
    private final Random random;
    private final Scanner stdInScanner;

    public GivenQuiz(Scanner stdInScanner) {
        this.random = new Random();
        this.stdInScanner = stdInScanner;
    }

    public void beginQuiz(QuizConfiguration config, DataSource ds, int timerMinutes) {
        if (config.getMaxEntry() - config.getMinEntry() < 1) {
            System.out.println("Invalid min / max");
            return;
        }
        List<PaoEntry> entries = ds.getEntries(config);

        if (entries.isEmpty()) {
            System.out.println("No entries in range " + config.getMinEntry() + ".." + config.getMaxEntry());
            return;
        }
        Collections.shuffle(entries);
        System.out.println();
        int questionsAskedInCurrentSet = 0;

        QuizStatistics stats = new QuizStatistics();
        if (timerMinutes > 0) {
            stats.startTimer();
            System.out.println("Timer started: " + timerMinutes + " minute" + (timerMinutes == 1 ? "" : "s"));
        }


        while (true) {
            questionsAskedInCurrentSet = questionsAskedInCurrentSet % entries.size();
            if (questionsAskedInCurrentSet == 0) {
                Collections.shuffle(entries);
            }

            EntryType questionGivenEntryType = config.getGivenEntryType();
            EntryType questionAnswerEntryType = config.getAnswerEntryType();
            if (questionGivenEntryType == EntryType.RANDOM) {
                questionGivenEntryType = getRandomEntryType(null);
            }
            if (questionAnswerEntryType == EntryType.RANDOM) {
                questionAnswerEntryType = getRandomEntryType(questionGivenEntryType);
            }

            PaoEntry entry = entries.get(questionsAskedInCurrentSet);
            String t1Word = questionAnswerEntryType.name();
            String t1Value = entry.getValue(questionAnswerEntryType);
            String t2Word = questionGivenEntryType.name();
            String t2Value = entry.getValue(questionGivenEntryType);
            System.out.print(color(PURPLE, t1Word) + " for " + color(CYAN, t2Word + " " + t2Value) + ": ");
            String answerText = stdInScanner.nextLine();

            QuestionContext questionContext = new QuestionContext();
            questionContext.setCorrectAnswer(entry.getValue(questionAnswerEntryType));
            questionContext.setEntry(questionGivenEntryType, entry);
            questionContext.setEntry(questionAnswerEntryType, entry);
            questionContext.setUserAnswerText(answerText);
            validateAnswer(questionContext, questionAnswerEntryType);

            // Handle if user types a quit command as an answer
            Command command = Command.getCommand(answerText);
            switch (command) {
                case QUIT -> {
                    return;
                }
                case EXIT -> System.exit(0);
                case null, default -> { }
            }

            // Record statistics
            stats.recordAnswer(questionContext.isCorrect());

            if (questionContext.isCorrect()) {
                String extraText = questionContext.isExactlyCorrect() ? "" : (" " + color(CYAN, questionContext.getCorrectAnswer()));
                System.out.println(color(GREEN, "Correct" + extraText));
            } else {
                System.out.println(color(RED, "Wrong:   " + t2Value + " has " + t1Word + " ") + color(CYAN, t1Value));
            }

            // Check if timer has expired
            if (stats.hasTimerExpired(timerMinutes)) {
                stats.printSummary();
                return;
            }

            System.out.println(System.lineSeparator());
            questionsAskedInCurrentSet++;
        }
    }

    private void validateAnswer(QuestionContext questionContext, EntryType answerType) {
        List<String> answerList = questionContext.getEntry(answerType).getAllByType(answerType);
        int bestCorrectWords = -1;
        int wrongWords = 0;
        String[] userAnswerTokens = questionContext.getUserAnswerText().split(" ");
        List<String> bestMatchedAnswer = Collections.emptyList();

        for (String possibleAnswer : answerList) {
            List<String> possibleAnswerTokens = Arrays.asList(possibleAnswer.split(" "));
            int correctWords = 0;
            for (String userAnswerToken : userAnswerTokens) {
                correctWords += possibleAnswerTokens.contains(userAnswerToken.toLowerCase()) ? 1 : 0;
            }
            int newWrongWords = Math.max(possibleAnswerTokens.size(), userAnswerTokens.length) - correctWords;
            if (correctWords > bestCorrectWords || (correctWords == bestCorrectWords && newWrongWords < wrongWords)) {
                bestCorrectWords = correctWords;
                wrongWords = Math.max(possibleAnswerTokens.size(), userAnswerTokens.length) - correctWords;
                bestMatchedAnswer = possibleAnswerTokens;
            }
        }
        // Allow one word wrong if the answer is longer, probably got the gist of it
        int allowedWordsWrong = bestMatchedAnswer.size() > 2 ? 1 : 0;

        questionContext.setCorrect(wrongWords <= allowedWordsWrong);
        questionContext.setExactlyCorrect(questionContext.getUserAnswerText().equalsIgnoreCase(answerList.getFirst()));
    }

    private EntryType getRandomEntryType(EntryType excludingType) {
        List<EntryType> entries = RANDOM_ENTRY_TYPES;
        if(excludingType != null) {
            entries = entries.stream().filter(entryType -> entryType != excludingType).toList();
        }
        return entries.get(random.nextInt(entries.size()));
    }

}
