# Wordament Game - WebSocket Communication Protocol

This document defines the WebSocket communication protocol between the Wordament game server and the clients.

## 1. Connection

Clients must connect to the server using the following WebSocket endpoint:

`ws://<your-server-address>/ws`

## 2. Game Flow

1.  A player (Player 1) sends a `create_game` message.
2.  The server creates a new game, generates a board, and responds to Player 1 with `game_created`.
3.  Another player (Player 2) sends a `join_game` message with the `gameId`.
4.  The server adds Player 2 to the game.
5.  The server sends `player_joined` to both players.
6.  The server sends `game_started` to both players, indicating whose turn it is to start.
7.  The current player sends a `submit_word` message.
8.  The server validates the word:
    *   If valid, it updates the score and broadcasts `word_submitted`, followed by `next_turn`.
    *   If invalid (not in the grid, already found, or not the player's turn), it sends an `error` message to the originating player.
9.  If a player disconnects, the server broadcasts `player_disconnected` and the game ends.

---

## 3. Messages from Client to Server

All messages from the client must be a JSON object with `type` and `payload` properties.

### `create_game`

Sent by a player to create a new game.

-   **Type:** `create_game`
-   **Payload:**
    ```json
    {
      "playerName": "Alice",
      "gridSize": 4
    }
    ```

### `join_game`

Sent by a player to join an existing game.

-   **Type:** `join_game`
-   **Payload:**
    ```json
    {
      "gameId": "some-game-id",
      "playerName": "Bob"
    }
    ```

### `submit_word`

Sent by a player during their turn to submit a word.

-   **Type:** `submit_word`
-   **Payload:**
    ```json
    {
      "word": "HELLO"
    }
    ```

---

## 4. Messages from Server to Client

All messages from the server will be a JSON object with `event` and `payload` properties.

### `game_created`

Sent to the player who created the game.

-   **Event:** `game_created`
-   **Payload:**
    ```json
    {
      "gameId": "some-game-id",
      "playerId": "player-1-session-id",
      "grid": [
        ["W", "O", "R", "D"],
        ["G", "A", "M", "E"],
        ["F", "U", "N", "X"],
        ["Y", "Z", "A", "B"]
      ]
    }
    ```

### `player_joined`

Notifies players that an opponent has joined.

-   **Event:** `player_joined`
-   **Payload (sent to the new player):**
    ```json
    {
      "opponent": {
        "id": "player-1-session-id",
        "name": "Alice",
        "score": 0
      },
      "grid": [
        ["W", "O", "R", "D"],
        // ...
      ]
    }
    ```
-   **Payload (sent to the existing player):**
    ```json
    {
      "opponent": {
        "id": "player-2-session-id",
        "name": "Bob",
        "score": 0
      }
    }
    ```

### `game_started`

Sent to both players when the game begins.

-   **Event:** `game_started`
-   **Payload:**
    ```json
    {
      "startingPlayerId": "player-1-session-id"
    }
    ```

### `word_submitted`

Broadcast to both players when a valid word is submitted.

-   **Event:** `word_submitted`
-   **Payload:**
    ```json
    {
      "playerId": "player-1-session-id",
      "word": "WORD",
      "score": 4
    }
    ```

### `next_turn`

Broadcast to both players to indicate the next turn.

-   **Event:** `next_turn`
-   **Payload:**
    ```json
    {
      "nextPlayerId": "player-2-session-id"
    }
    ```

### `player_disconnected`

Broadcast to the remaining player when an opponent disconnects. The game is terminated on the server.

-   **Event:** `player_disconnected`
-   **Payload:**
    ```json
    {
      "disconnectedPlayerId": "player-2-session-id"
    }
    ```

### `error`

Sent to a player when an action they took is invalid.

-   **Event:** `error`
-   **Payload:**
    ```json
    {
      "message": "It's not your turn"
    }
    ```
