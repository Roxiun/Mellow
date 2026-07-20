package com.roxiun.mellow.util.localdenick;

public class LocalDenickedPlayer {

    private final String name;
    private final String nick;

    public LocalDenickedPlayer(String name, String nick) {
        this.name = name;
        this.nick = nick;
    }

    public String getName() {
        return name;
    }

    public String getNick() {
        return nick;
    }
}
