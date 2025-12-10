package com.spyder.pao;

import com.spyder.pao.model.*;

import java.util.*;

import static com.spyder.pao.ConsoleColors.*;

public class GivenQuiz {

    private final List<EntryType> RANDOM_ENTRY_TYPES = Arrays.asList(EntryType.PERSON, EntryType.ACTION, EntryType.OBJECT);
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
                System.out.printf(color(RED, "Wrong:   [%s] %s has %s ") + color(CYAN, t1Value) + "%n", entry.getNumberStr(), t2Value, t1Word);
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
        String userInput = questionContext.getUserAnswerText();
        String[] userInputTokens = userInput.split(" ");
        
        // Check if first token is a number and validate it if so
        boolean numberCorrect = true;
        String answerTextForValidation = userInput;
        
        if (userInputTokens.length > 0 && userInputTokens[0].matches("\\d+")) {
            // First token is a number, validate it
            String userNumber = userInputTokens[0];
            String expectedNumber = questionContext.getEntry(answerType).getNumberStr();
            numberCorrect = userNumber.equals(expectedNumber);
            
            // Use remaining tokens as the answer (if answer type is not NUMBER)
            if (answerType != EntryType.NUMBER && userInputTokens.length > 1) {
                answerTextForValidation = String.join(" ", Arrays.copyOfRange(userInputTokens, 1, userInputTokens.length));
            }
        }
        
        // Validate the entity answer
        List<String> answerList = questionContext.getEntry(answerType).getAllByType(answerType);
        int bestCorrectWords = -1;
        int wrongWords = 0;
        String[] userAnswerTokens = answerTextForValidation.split(" ");
        List<String> bestMatchedAnswer = Collections.emptyList();

        for (String possibleAnswer : answerList) {
            List<String> possibleAnswerTokens = Arrays.asList(possibleAnswer.split(" "));
            int correctWords = 0;
            for (String userAnswerToken : userAnswerTokens) {
                boolean wordMatches = possibleAnswerTokens.stream()
                        .anyMatch(possibleToken -> isWordCloseEnough(userAnswerToken, possibleToken));
                correctWords += wordMatches ? 1 : 0;
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

        boolean entityCorrect = wrongWords <= allowedWordsWrong;
        questionContext.setCorrect(numberCorrect && entityCorrect);
        questionContext.setExactlyCorrect(answerTextForValidation.equalsIgnoreCase(answerList.getFirst()));
    }

    private EntryType getRandomEntryType(EntryType excludingType) {
        List<EntryType> entries = RANDOM_ENTRY_TYPES;
        if(excludingType != null) {
            entries = entries.stream().filter(entryType -> entryType != excludingType).toList();
        }
        return entries.get(random.nextInt(entries.size()));
    }

    /**
     * Checks if two words are close enough to be considered a match.
     * Returns true if words are exactly equal (case-insensitive) or have an edit distance of 1.
     * Edit distance of 1 means: one character added, removed, or substituted.
     */
    private boolean isWordCloseEnough(String userWord, String targetWord) {
        String user = userWord.toLowerCase();
        String target = targetWord.toLowerCase();

        // Exact match
        if (user.equals(target)) {
            return true;
        }

        int lenDiff = Math.abs(user.length() - target.length());

        // Edit distance can't be 1 if length difference is > 1
        if (lenDiff > 1) {
            return false;
        }

        // Check for edit distance of 1
        if (lenDiff == 0) {
            // Same length - check for one substitution
            int differences = 0;
            for (int i = 0; i < user.length(); i++) {
                if (user.charAt(i) != target.charAt(i)) {
                    differences++;
                    if (differences > 1) {
                        return false;
                    }
                }
            }
            return differences == 1;
        } else {
            // Length differs by 1 - check for one insertion/deletion
            String shorter = user.length() < target.length() ? user : target;
            String longer = user.length() < target.length() ? target : user;

            int i = 0, j = 0;
            boolean foundDifference = false;

            while (i < shorter.length() && j < longer.length()) {
                if (shorter.charAt(i) != longer.charAt(j)) {
                    if (foundDifference) {
                        return false;
                    }
                    foundDifference = true;
                    j++; // Skip character in longer string
                } else {
                    i++;
                    j++;
                }
            }
            return true;
        }
    }

}
