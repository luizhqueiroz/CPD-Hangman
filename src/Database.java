import java.io.*;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.mindrot.jbcrypt.BCrypt;

public class Database {
    private final File file;
    private final Gson gson;

    public Database(String filename) {
        this.file = new File(filename);
        this.gson = new Gson();

        if (!file.exists()) {
            this.createEmptyFile();
        }
    }

    private void createEmptyFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.print("[]");
        } catch (IOException e) {
            System.out.println("Unable to create an empty file: " + e.getMessage());
        }
    }

    private List<Player> getAllPlayers() {
        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, new TypeToken<List<Player>>() {
            }.getType());
        } catch (IOException e) {
            System.out.println("Error reading players: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private Player getPlayer(String username) {
        List<Player> players = this.getAllPlayers();

        for (Player player : players) {
            if (player.getUsername().equals(username)) return player;
        }
        return null;
    }

    private void savePlayers(List<Player> players) {
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(players, writer);
        } catch (IOException e) {
            System.out.println("Error writing players to file: " + e.getMessage());
        }
    }

    private void savePlayer(Player player) {
        List<Player> players = this.getAllPlayers();
        players.add(player);
        this.savePlayers(players);
    }

    public void updatePlayers(List<Player> players) {
        for (Player player : players) {
            this.updatePlayer(player);
        }
    }

    public void updatePlayer(Player player) {
        List<Player> players = this.getAllPlayers();
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getUsername().equals(player.getUsername())) {
                players.set(i, player);
                break;
            }
        }

        this.savePlayers(players);
    }


    public boolean register(String username, String password) {
        if (this.getPlayer(username) != null) {
            return false;
        } else {
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            Player player = new Player(username, hashedPassword, "", 0, null);
            this.savePlayer(player);
        }

        return true;
    }

    public Player auth(String username, String password, String token) {
        Player player = this.getPlayer(username);
        if (player != null && BCrypt.checkpw(password, player.getPassword())) {
            player.setToken(token);
            this.updatePlayer(player);
            return player;
        }

        return null;
    }

    public Player authToken(String token) {
        List<Player> players = this.getAllPlayers();

        for (Player player : players) {
            if (player.getToken().equals(token)) return player;
        }

        return null;
    }

    public void resetAllToken() {
        List<Player> players = this.getAllPlayers();

        for (Player player : players) {
            player.setToken("");
            this.updatePlayer(player);
        }
    }
}
