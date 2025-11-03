package com.spyder.pao.model;

import java.util.HashMap;
import java.util.Map;

public enum Command {

    // Command constructor parameter is the word(s) that triggers that command
    QUIT("quit"),
    EXIT("exit"),
    HELP("help"),
    LIST("list"),
    FROM("from"),
    TO("to"),
    QUIZ_TYPE("quiz"),
    GIVEN("given"),
    ANSWER("answer"),
    TIMER("timer"),
    BEGIN_QUIZ(new String[] {"begin", "b", "start"}),
    ;

    private final String[] triggers;

    Command(String trigger) {
        this(new String[] {trigger});
    }
    
    Command(String[] triggers) {
        this.triggers = triggers;
    }

    // Map trigger -> command, ie "quiz" -> QUIZ_TYPE
    private static final Map<String, Command> map = new HashMap<>();

    static {
        initCommandMap();
    }

    private static void initCommandMap() {
        for(Command command : Command.values()) {
            for(String trigger : command.triggers) {
                map.put(trigger, command);
            }
        }
    }

    public static Command getCommand(String trigger) {
        return map.get(trigger);
    }
}
