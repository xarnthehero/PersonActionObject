package com.spyder.pao.model;

import lombok.Data;

@Data
public class QuizStatistics {
    private int totalAnswers = 0;
    private int correctAnswers = 0;
    private long startTimeMillis;
    private long endTimeMillis;

    public void recordAnswer(boolean correct) {
        totalAnswers++;
        if (correct) {
            correctAnswers++;
        }
    }

    public void startTimer() {
        startTimeMillis = System.currentTimeMillis();
    }

    public void endTimer() {
        endTimeMillis = System.currentTimeMillis();
    }

    public boolean hasTimerExpired(int timerMinutes) {
        if (timerMinutes <= 0) {
            return false;
        }
        long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
        return elapsedMillis >= (timerMinutes * 60L * 1000L);
    }

    public void printSummary() {
        System.out.println(System.lineSeparator() + "Time's up!");
        if(correctAnswers > 0) {
            System.out.printf("%d correct out of %d total answers%n", correctAnswers, totalAnswers);
        } else {
            System.out.printf("%s questions answered", totalAnswers);
        }
        System.out.println();
    }
}
