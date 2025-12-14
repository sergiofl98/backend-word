package com.example.demo.game;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
public class Dictionary {

    private static final String DICTIONARY_PATH = "/spanish_words.txt";
    private final Set<String> words = new HashSet<>();

    @PostConstruct
    public void init() {
        try (InputStream inputStream = Dictionary.class.getResourceAsStream(DICTIONARY_PATH);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                words.add(line.trim().toUpperCase());
            }
        } catch (Exception e) {
            System.err.println("Error loading dictionary: " + e.getMessage());
        }
    }

    public boolean isValidWord(String word) {
        return words.contains(word.toUpperCase());
    }

    public Set<String> getWords() {
        return Collections.unmodifiableSet(words);
    }
}
