import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

public class Player {
    private final String username;
    private final String password;
    private String token;
    private int currentLevel;
    private transient Socket socket;
    private static final int RECEIVE_TIMEOUT_MS = 20000;

    public Player(String username, String password, String token, int currentLevel, Socket socket) {
        this.username = username;
        this.password = password;
        this.token = token;
        this.currentLevel = currentLevel;
        this.socket = socket;

    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getToken() {
        return this.token;
    }

    public int getCurrentLevel() {
        return this.currentLevel;
    }

    public Socket getSocket() {
        return this.socket;
    }

    public void setCurrentLevel(int level) {
        this.currentLevel = level;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public void send(String message) throws IOException {
        PrintWriter writer = new PrintWriter(this.socket.getOutputStream(), true);
        writer.println(message);
    }

    public String receive() throws IOException, TimeoutException {
        this.socket.setSoTimeout(RECEIVE_TIMEOUT_MS); // Set socket timeout

        BufferedReader reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        try {
            return reader.readLine();
        } catch (SocketTimeoutException e) {
            System.out.println("SERVER Player.Java - Player did not respond within the timeout period.");
            throw new TimeoutException("Player did not respond within the timeout period.");
        } finally {
            this.socket.setSoTimeout(0);
        }
    }

    public boolean equals(Player player) {
        return this.username.equals(player.getUsername());
    }
}
