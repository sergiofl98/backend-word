package com.example.demo.model;

import com.example.demo.game.Dictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Game {

    private final String id;
    private final Board board;
    private final List<Player> players;
    private final Set<String> foundWords;
    private int currentPlayerIndex;


    public Game(String id, int gridSize, Dictionary dictionary) {
        this.id = id;
        this.board = new Board(gridSize, dictionary);
        this.players = new ArrayList<>();
        this.foundWords = new HashSet<>();
        this.currentPlayerIndex = 0;
    }

    public String getId() {
        return id;
    }

    public Board getBoard() {
        return board;
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public void addPlayer(Player player) {
        if (players.size() < 2) {
            players.add(player);
        }
    }

    public boolean isWordFound(String word) {
        return foundWords.contains(word.toUpperCase());
    }

    public void addFoundWord(String word) {
        foundWords.add(word.toUpperCase());
    }

    public Player getCurrentPlayer() {
        if (players.isEmpty()) {
            return null;
        }
        return players.get(currentPlayerIndex);
    }

    public void nextTurn() {
        if (!players.isEmpty()) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        }
    }
}
