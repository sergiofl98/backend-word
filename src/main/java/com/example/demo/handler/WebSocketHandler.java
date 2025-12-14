package com.example.demo.handler;

import com.example.demo.game.Dictionary;
import com.example.demo.game.GameService;
import com.example.demo.model.Game;
import com.example.demo.model.Player;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final GameService gameService = GameService.getInstance();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Dictionary dictionary;

    @Autowired
    public WebSocketHandler(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        System.out.println("New WebSocket connection: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(message.getPayload());
        String messageType = jsonNode.get("type").asText();

        switch (messageType) {
            case "create_game":
                handleCreateGame(session, jsonNode.get("payload"));
                break;
            case "join_game":
                handleJoinGame(session, jsonNode.get("payload"));
                break;
            case "submit_word":
                handleSubmitWord(session, jsonNode.get("payload"));
                break;
            default:
                System.out.println("Unknown message type: " + messageType);
        }
    }

    private void handleCreateGame(WebSocketSession session, JsonNode payload) throws IOException {
        int gridSize = payload.get("gridSize").asInt();
        String playerName = payload.get("playerName").asText();

        Game game = gameService.createGame(gridSize, dictionary);
        Player player = new Player(session.getId(), playerName);
        game.addPlayer(player);

        session.getAttributes().put("gameId", game.getId());

        ObjectNode responsePayload = objectMapper.createObjectNode();
        responsePayload.put("gameId", game.getId());
        responsePayload.put("playerId", player.getId());
        responsePayload.set("grid", objectMapper.valueToTree(game.getBoard().getGrid()));

        sendMessage(session, "game_created", responsePayload);
    }

    private void handleJoinGame(WebSocketSession session, JsonNode payload) throws IOException {
        String gameId = payload.get("gameId").asText();
        String playerName = payload.get("playerName").asText();

        Game game = gameService.getGame(gameId);
        if (game == null) {
            sendError(session, "Game not found");
            return;
        }

        if (game.getPlayers().size() >= 2) {
            sendError(session, "Game is full");
            return;
        }

        Player newPlayer = new Player(session.getId(), playerName);
        game.addPlayer(newPlayer);
        session.getAttributes().put("gameId", game.getId());

        // Notify existing player
        Player existingPlayer = game.getPlayers().get(0);
        WebSocketSession existingPlayerSession = sessions.get(existingPlayer.getId());
        if (existingPlayerSession != null) {
            ObjectNode payloadForExisting = objectMapper.createObjectNode();
            payloadForExisting.set("opponent", objectMapper.valueToTree(newPlayer));
            sendMessage(existingPlayerSession, "player_joined", payloadForExisting);
        }

        // Notify new player
        ObjectNode payloadForNew = objectMapper.createObjectNode();
        payloadForNew.set("opponent", objectMapper.valueToTree(existingPlayer));
        payloadForNew.set("grid", objectMapper.valueToTree(game.getBoard().getGrid()));
        sendMessage(session, "player_joined", payloadForNew);

        // Start game
        ObjectNode gameStartedPayload = objectMapper.createObjectNode();
        gameStartedPayload.put("startingPlayerId", existingPlayer.getId());
        broadcast(game, "game_started", gameStartedPayload);
    }

    private void handleSubmitWord(WebSocketSession session, JsonNode payload) throws IOException {
        String gameId = (String) session.getAttributes().get("gameId");
        Game game = gameService.getGame(gameId);

        if (game == null) {
            sendError(session, "Game not found");
            return;
        }

        Player currentPlayer = game.getCurrentPlayer();
        if (!currentPlayer.getId().equals(session.getId())) {
            sendError(session, "It's not your turn");
            return;
        }

        String word = payload.get("word").asText().toUpperCase();

        if (!dictionary.isValidWord(word)) {
            sendError(session, "Word not in dictionary");
            return;
        }

        if (game.isWordFound(word)) {
            sendError(session, "Word already found");
            return;
        }

        if (!game.getBoard().isValidWord(word)) {
            sendError(session, "Invalid word");
            return;
        }

        game.addFoundWord(word);
        int points = word.length(); // Simple scoring: 1 point per letter
        currentPlayer.addScore(points);

        ObjectNode wordSubmittedPayload = objectMapper.createObjectNode();
        wordSubmittedPayload.put("playerId", currentPlayer.getId());
        wordSubmittedPayload.put("word", word);
        wordSubmittedPayload.put("score", currentPlayer.getScore());
        broadcast(game, "word_submitted", wordSubmittedPayload);

        game.nextTurn();

        ObjectNode nextTurnPayload = objectMapper.createObjectNode();
        nextTurnPayload.put("nextPlayerId", game.getCurrentPlayer().getId());
        broadcast(game, "next_turn", nextTurnPayload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        System.out.println("Closed WebSocket connection: " + session.getId());
        String gameId = (String) session.getAttributes().get("gameId");
        if (gameId != null) {
            Game game = gameService.getGame(gameId);
            if (game != null) {
                Player disconnectedPlayer = game.getPlayers().stream()
                        .filter(p -> p.getId().equals(session.getId()))
                        .findFirst()
                        .orElse(null);

                if (disconnectedPlayer != null) {
                    ObjectNode payload = objectMapper.createObjectNode();
                    payload.put("disconnectedPlayerId", disconnectedPlayer.getId());
                    broadcast(game, "player_disconnected", payload);
                    gameService.removeGame(gameId);
                }
            }
        }
        sessions.remove(session.getId());
    }

    private void sendMessage(WebSocketSession session, String event, JsonNode payload) throws IOException {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("event", event);
        message.set("payload", payload);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
    }

    private void broadcast(Game game, String event, JsonNode payload) throws IOException {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("event", event);
        message.set("payload", payload);
        String jsonMessage = objectMapper.writeValueAsString(message);

        for (Player player : game.getPlayers()) {
            WebSocketSession playerSession = sessions.get(player.getId());
            if (playerSession != null && playerSession.isOpen()) {
                playerSession.sendMessage(new TextMessage(jsonMessage));
            }
        }
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        ObjectNode errorPayload = objectMapper.createObjectNode();
        errorPayload.put("message", message);
        sendMessage(session, "error", errorPayload);
    }
}
