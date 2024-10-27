import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class Game {

    private final List<Player> players;
    private int playersNumber;
    private static final String[] words = {"system", "distributed", "assignment", "programming"};
    private final String word = words[(int) (Math.random() * words.length)];
    private String result = new String(new char[word.length()]).replace("\0", "*");
    private final int[] scores;
    private int attempts;

    public Game(List<Player> players) {
        this.players = players;
        this.playersNumber = players.size();
        this.scores = new int[this.playersNumber];
        this.attempts = 8;
    }

    public void start() throws IOException, TimeoutException {
        System.out.println("Starting game with " + this.players.size() + " players");
        this.notifyAllPlayers("START-Game Started!");

        if (this.playersNumber <= 0) {
            System.out.println("Insufficient players to initiate the game");
            return;
        }

        while (this.attempts > 0 && this.result.contains("*")) {

            for (int i = 0; i < this.playersNumber; i++) {
                if (this.attempts <= 0)
                    break;
                System.out.println("New ROUND!");
                Player player = this.players.get(i);
                this.scoreBoard();
                this.notifyAllPlayers("INFO-It's player " + player.getUsername() + " turn");
                this.notifyAllPlayers("INFO-The word is: " + this.result);
                this.notifyPlayer(player, "GUESS-Guess a letter!");
                String guess;
                try {
                    System.out.println("Waiting for player " + player.getUsername() + " to guess the word");
                    String received = player.receive();
                    if (received != null) {
                        guess = received.split("-", 2)[1];
                        System.out.println("Player " + player.getUsername() + " guessed: " + guess);
                    } else {
                        System.out.println("Player " + player.getUsername() + " sent null response, continuing.");
                        continue;
                    }
                } catch (TimeoutException e) {
                    this.attempts--;
                    this.notifyAllPlayers("INFO-timeout");
                    this.notifyAllPlayers("INFO-Timed Out! Number of attempts left: " + this.attempts);
                    System.out.println("Player " + player.getUsername() + " did not respond in time, continuing.");
                    continue;
                } catch (IOException e) {
                    System.out.println("Player " + player.getUsername() + " has disconnected, continuing with remaining players.");
                    this.players.remove(i);
                    this.playersNumber--;
                    i--;
                    continue;
                }
                if (this.isGuessRight(guess)) {
                    if (this.isWordGuessed()) {
                        this.scores[i] += 20;
                        player.setCurrentLevel(player.getCurrentLevel() + 20);
                        this.notifyAllPlayers("INFO-Well Done! Player " + player.getUsername() + " guessed the word and scored 20 points!");
                        this.notifyAllPlayers("INFO-The word was " + this.word.toUpperCase());
                        break;
                    }

                    this.notifyAllPlayers("INFO-Player " + player.getUsername() + " guessed right and scored 5 points!");
                    this.scores[i] += 5;
                    player.setCurrentLevel(player.getCurrentLevel() + 5);
                } else {
                    this.attempts--;
                    this.warning();
                }
            }
        }
        this.scoreBoard();
    }

    private boolean isGuessRight(String guess) {
        if (guess == null || guess.isEmpty()) {
            return false;
        }

        StringBuilder newResult = new StringBuilder();

        for (int i = 0; i < this.word.length(); i++) {
            if (this.word.charAt(i) == guess.charAt(0)) {
                newResult.append(guess.charAt(0));
            } else {
                newResult.append(this.result.charAt(i));
            }
        }

        if (this.result.equals(newResult.toString())) {
            return false;
        } else {
            this.result = newResult.toString();
        }

        return true;
    }

    private boolean isWordGuessed() {
        return this.result.equals(this.word);
    }

    private void scoreBoard() {
        StringBuilder board = new StringBuilder();

        board.append("INFO-Scoreboard: ");
        for (int i = 0; i < this.playersNumber; i++) {
            board.append("Player ").append(this.players.get(i).getUsername()).append(": ").append(this.scores[i]).append(" | ");
        }

        this.notifyAllPlayers(board.toString());
    }

    private void warning() {
        if (this.attempts > 0) {
            this.notifyAllPlayers("INFO-Wrong guess! Number of attempts left: " + this.attempts);
        } else {
            this.notifyAllPlayers("INFO-GAME OVER! No one could guess the word. The word was " + this.word + ".");
        }
    }

    private void notifyAllPlayers(String message) {
        try {
            for (Player player : this.players) {
                player.send(message);
                player.receive();
            }
        } catch (Exception e) {
            System.out.println("Error notifying players: " + e.getMessage());
        }
    }

    private void notifyPlayer(Player player, String message) {
        try {
            System.out.println("Notifying player " + player.getUsername() + ": " + message);
            player.send(message);
        } catch (Exception e) {
            System.out.println("Error notifying player" + player.getUsername() + ": " + e.getMessage());
        }
    }

    public List<Player> getPlayers() {
        return this.players;
    }
}
