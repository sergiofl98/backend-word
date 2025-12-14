package com.example.demo.game;

import com.example.demo.model.Game;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    private static final GameService INSTANCE = new GameService();
    private final Map<String, Game> games = new ConcurrentHashMap<>();

    private GameService() {
    }

    public static GameService getInstance() {
        return INSTANCE;
    }

    public Game createGame(int gridSize, Dictionary dictionary) {
        String gameId = UUID.randomUUID().toString();
        Game game = new Game(gameId, gridSize, dictionary);
        games.put(gameId, game);
        return game;
    }

    public Game getGame(String gameId) {
        return games.get(gameId);
    }

    public void removeGame(String gameId) {
        games.remove(gameId);
    }
}
