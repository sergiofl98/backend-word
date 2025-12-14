package com.example.demo.model;

import com.example.demo.game.Dictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Board {

    private final char[][] grid;
    private final int size;
    private final Dictionary dictionary;

    public Board(int size, Dictionary dictionary) {
        this.size = size;
        this.grid = new char[size][size];
        this.dictionary = dictionary;
        generate();
    }

    public char[][] getGrid() {
        return grid;
    }

    private void generate() {
        // A simple board generation heuristic
        // 1. Try to place a few random words from the dictionary
        // 2. Fill the rest of the board with random letters

        Random random = new Random();
        Set<String> words = dictionary.getWords();
        List<String> wordList = new ArrayList<>(words);

        int wordsToPlace = size; // Try to place `size` words

        for (int i = 0; i < wordsToPlace; i++) {
            String word = wordList.get(random.nextInt(wordList.size()));
            if (word.length() <= size) {
                placeWord(word);
            }
        }

        // Fill the rest
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (grid[i][j] == '\0') { // If cell is empty
                    grid[i][j] = (char) ('A' + random.nextInt(26));
                }
            }
        }
    }

    private void placeWord(String word) {
        Random random = new Random();
        int row = random.nextInt(size);
        int col = random.nextInt(size);
        boolean horizontal = random.nextBoolean();

        if (canPlace(word, row, col, horizontal)) {
            for (int i = 0; i < word.length(); i++) {
                if (horizontal) {
                    grid[row][col + i] = word.charAt(i);
                } else {
                    grid[row + i][col] = word.charAt(i);
                }
            }
        }
    }

    private boolean canPlace(String word, int row, int col, boolean horizontal) {
        if (horizontal) {
            if (col + word.length() > size) {
                return false;
            }
            for (int i = 0; i < word.length(); i++) {
                if (grid[row][col + i] != '\0') {
                    return false;
                }
            }
        } else {
            if (row + word.length() > size) {
                return false;
            }
            for (int i = 0; i < word.length(); i++) {
                if (grid[row + i][col] != '\0') {
                    return false;
                }
            }
        }
        return true;
    }


    public boolean isValidWord(String word) {
        if (word == null || word.isEmpty()) {
            return false;
        }

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (grid[i][j] == word.charAt(0)) {
                    if (search(word, i, j)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean search(String word, int row, int col) {
        boolean[][] visited = new boolean[size][size];
        return dfs(word, 0, row, col, visited);
    }

    private boolean dfs(String word, int index, int row, int col, boolean[][] visited) {
        if (index == word.length()) {
            return true;
        }

        if (row < 0 || row >= size || col < 0 || col >= size || visited[row][col] || grid[row][col] != word.charAt(index)) {
            return false;
        }

        visited[row][col] = true;

        int[] dr = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dc = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int i = 0; i < 8; i++) {
            if (dfs(word, index + 1, row + dr[i], col + dc[i], visited)) {
                return true;
            }
        }

        visited[row][col] = false; // Backtrack

        return false;
    }
}
