import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;


public class Server {
    private final int port;
    private final int mode;
    private SSLServerSocket serverSocket;
    private final Database database;
    private final ReentrantLock database_lock;
    private final Queue<Player> queue;
    private final List<Player> queueRank;
    private final ReentrantLock queue_lock;
    private long lastTime;
    private int levelDifference;
    private static final int PLAYERS_PER_GAME = 3;
    private static final int DEFAULT_LEVEL_DIFFERENCE = 20;
    private static final long DEFAULT_RELAXED_TIME_SEC = 30;
    private static final String DATABASE_PATH = "database/";
    private static final String DATABASE_FILENAME = "database.json";


    public Server(int port, int mode, String filename) {
        this.port = port;
        this.mode = mode;
        this.database = new Database(DATABASE_PATH + filename);
        this.database_lock = new ReentrantLock();
        this.queue = new LinkedList<>();
        this.queueRank = new ArrayList<>();
        this.queue_lock = new ReentrantLock();
        this.levelDifference = DEFAULT_LEVEL_DIFFERENCE;
    }

    private static void printUsage() {
        System.out.println("Usage: Server <PORT> <MODE> [DATABASE]");
        System.out.println("where:");
        System.out.println("<PORT> is an integer");
        System.out.println("<MODE> is an integer: 0 for Simple Mode, 1 for Ranking Mode");
        System.out.println("[DATABASE] is the database filename");
    }

    private void start() throws IOException {
        ServerSocketFactory factory = SSLServerSocketFactory.getDefault();
        serverSocket = (SSLServerSocket) factory.createServerSocket(port);
        serverSocket.setNeedClientAuth(true);

        System.out.println("Server is running on port " + port);

        try {
            this.run();
        } catch (Exception e) {
            System.out.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void connectionHandler() {
        while (true) {
            try {
                Socket socket = this.serverSocket.accept();
                System.out.println("New player connected");
                Thread.ofVirtual().start(() -> {
                    try {
                        this.clientHandler(socket);
                    } catch (IOException e) {
                        System.out.println("Error handling a client: " + e.getMessage());
                    }
                });
            } catch (IOException e) {
                System.out.println("Error accepting connection from a client: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void gameLobbyHandlerSimple() {
        while (true) {
            List<Player> players = new ArrayList<>();
            this.queue_lock.lock();
            if (this.queue.size() >= PLAYERS_PER_GAME) {
                for (int i = 0; i < PLAYERS_PER_GAME; i++) {
                    Player player = this.queue.poll();
                    try {
                        player.send("PING-Checking connection");
                        String answer = player.receive();
                        if (answer != null) {
                            players.add(player);
                        } else {
                            player.setToken("");

                            this.database_lock.lock();
                            this.database.updatePlayer(player);
                            this.database_lock.unlock();

                            player.getSocket().close();
                        }
                    } catch (IOException e) {
                        System.out.println("Player " + player.getUsername() + " is not active");
                        player.setToken("");

                        this.database_lock.lock();
                        this.database.updatePlayer(player);
                        this.database_lock.unlock();
                        try {
                            player.getSocket().close();
                        } catch (IOException ex) {
                            System.out.println("I/O error: " + ex.getMessage());
                        }
                    } catch (TimeoutException e) {
                        throw new RuntimeException(e);
                    }
                }

                Thread.ofVirtual().start(() -> this.gameHandler(players));
            }
            this.queue_lock.unlock();
        }
    }

    private void gameLobbyHandlerRank() {
        this.lastTime = System.currentTimeMillis();
        while (true) {
            List<Player> players = new ArrayList<>();

            int i = 0;
            this.queue_lock.lock();
            this.sortQueue();
            while (i <= this.queueRank.size() - PLAYERS_PER_GAME) {
                Player first = this.queueRank.get(i);
                Player last = this.queueRank.get(i + PLAYERS_PER_GAME - 1);

                if (this.isAcceptableLevelDifference(first, last)) {
                    for (int j = 0; j < PLAYERS_PER_GAME; j++) {
                        Player player = this.queueRank.remove(i);

                        try {
                            player.send("PING-Checking connection");
                            String answer = player.receive();
                            if (answer != null) {
                                players.add(player);
                            } else {
                                player.setToken("");

                                this.database_lock.lock();
                                this.database.updatePlayer(player);
                                this.database_lock.unlock();
                                player.getSocket().close();
                            }
                        } catch (IOException e) {
                            System.out.println("Player " + player.getUsername() + " is not active");
                            player.setToken("");

                            this.database_lock.lock();
                            this.database.updatePlayer(player);
                            this.database_lock.unlock();
                            try {
                                player.getSocket().close();
                            } catch (IOException ex) {
                                System.out.println("I/O error: " + ex.getMessage());
                            }
                        } catch (TimeoutException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    Thread.ofVirtual().start(() -> this.gameHandler(players));
                } else {
                    i++;
                }
            }
            this.queue_lock.unlock();
        }
    }

    private boolean isAcceptableLevelDifference(Player player1, Player player2) {
        this.relaxDifference();
        if (Math.abs(player2.getCurrentLevel() - player1.getCurrentLevel()) <= this.levelDifference) {
            this.lastTime = System.currentTimeMillis();
            this.levelDifference = DEFAULT_LEVEL_DIFFERENCE;
            return true;
        }

        return false;
    }

    private void relaxDifference() {
        long actualTime = System.currentTimeMillis();
        long elapsedTime = (actualTime - this.lastTime) / 1000;
        if (elapsedTime > DEFAULT_RELAXED_TIME_SEC) {
            this.lastTime = System.currentTimeMillis();
            this.levelDifference += 10;
        }
    }

    private void sortQueue() {
        this.queue_lock.lock();
        this.queueRank.sort(Comparator.comparingInt(Player::getCurrentLevel));
        this.queue_lock.unlock();
    }

    private void gameHandler(List<Player> players) {
        try {
            Game game = new Game(players);
            game.start();

            List<Player> updatedPlayers = game.getPlayers();


            for (Player player : updatedPlayers) {
                player.send("GAMEOVER-Game is over. Do you want to play again? (Type yes or no)");
                String answer;
                try {
                    answer = player.receive().split("-", 2)[1].toLowerCase();
                } catch (TimeoutException e) {
                    answer = "no";
                }

                if (answer.equals("yes")) {
                    this.insertInQueue(player, false);
                } else {
                    player.send("END-Thank you for playing(Left or Timed Out)!");
                    player.setToken("");

                    this.database_lock.lock();
                    this.database.updatePlayer(player);
                    this.database_lock.unlock();
                    player.getSocket().close();
                }
            }
        } catch (IOException e) {
            System.out.println("I/O error:: " + e.getMessage());
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private void run() {
        this.database.resetAllToken();

        Thread connectionThread = new Thread(this::connectionHandler);
        Thread gameLobbyThread;
        if (this.mode == 0) {
            gameLobbyThread = new Thread(this::gameLobbyHandlerSimple);
        } else {
            gameLobbyThread = new Thread(this::gameLobbyHandlerRank);
        }

        connectionThread.start();
        gameLobbyThread.start();
    }

    private boolean register(String username, String password) {
        this.database_lock.lock();
        boolean isRegistered = this.database.register(username, password);
        this.database_lock.unlock();

        return isRegistered;
    }

    private Player auth(String username, String password) {
        String token = this.getToken();
        this.database_lock.lock();
        Player player = this.database.auth(username, password, token);
        this.database_lock.unlock();

        return player;
    }

    private Player authToken(String token) {
        this.database_lock.lock();
        Player player = this.database.authToken(token);
        this.database_lock.unlock();

        return player;
    }

    private String getToken() {
        return UUID.randomUUID().toString();
    }

    private boolean insertInQueue(Player player, boolean isReconnection) {
        if (this.mode == 0) {
            try {
                this.queue_lock.lock();
                if (isReconnection) {
                    for (Player p : this.queue) {
                        if (p.getToken().equals(player.getToken())) {
                            p.setSocket(player.getSocket());
                            System.out.println("Player " + player.getUsername() + " is already in the queue");
                            player.send("QUEUE-Back to the queue");
                            return true;
                        }
                    }
                } else {
                    for (Player p : this.queue) {
                        if (p.getUsername().equals(player.getUsername())) {
                            p.setSocket(player.getSocket());
                            System.out.println("Player " + player.getUsername() + " is already in the queue");
                            player.send("QUEUE-Already in the queue");
                            return true;
                        }
                    }

                    if (this.queue.offer(player)) {
                        player.send("QUEUE-Inserted in the queue");
                        System.out.println("Player " + player.getUsername() + " inserted in the queue");
                        return true;
                    }
                }
            } catch (IOException e) {
                System.out.println("Error inserting players in the queue: " + e.getMessage());
            } finally {
                this.queue_lock.unlock();
            }

        } else {
            try {
                this.queue_lock.lock();
                if (isReconnection) {
                    for (Player p : this.queueRank) {
                        if (p.getToken().equals(player.getToken())) {
                            p.setSocket(player.getSocket());
                            System.out.println("Player " + player.getUsername() + " is already in the queue");
                            player.send("QUEUE-Back to the queue");
                            return true;
                        }
                    }
                } else {
                    for (Player p : this.queueRank) {
                        if (p.getUsername().equals(player.getUsername())) {
                            p.setSocket(player.getSocket());
                            System.out.println("Player " + player.getUsername() + " is already in the queue");
                            player.send("QUEUE-Already in the queue");
                            return true;
                        }
                    }

                    this.queueRank.add(player);
                    player.send("QUEUE-Inserted in the queue");
                    System.out.println("Player " + player.getUsername() + " inserted in the queue");
                    return true;
                }
            } catch (IOException e) {
                System.out.println("Error inserting players in the queue: " + e.getMessage());
            } finally {
                this.queue_lock.unlock();
            }

        }

        return false;
    }

    private void clientHandler(Socket socket) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

        while (true) {
            String answer = reader.readLine();
            String code;
            String[] clientResponse = new String[2];
            if (answer != null) {
                clientResponse = answer.split("-", 2);
                code = clientResponse[0];
            } else {
                code = "NONE";
            }

            switch (code) {
                case "REG" -> {
                    String[] userData = clientResponse[1].split(" ");
                    if (this.register(userData[0], userData[1])) {
                        writer.println("ACK-Registration successful");
                    } else {
                        writer.println("NACK-Username already exists");
                    }
                }
                case "AUTH" -> {
                    String[] userData = clientResponse[1].split(" ");
                    Player player = this.auth(userData[0], userData[1]);
                    if (player != null) {
                        player.setSocket(socket);
                        writer.println("TOKEN-" + player.getToken());
                        this.insertInQueue(player, false);
                        return;
                    } else {
                        writer.println("NACK-Authentication failed");
                    }
                }
                case "TOKEN" -> {
                    String token = clientResponse[1];
                    Player player = this.authToken(token);
                    if (player != null) {
                        player.setSocket(socket);
                        if (this.insertInQueue(player, true)) {
                            return;
                        }
                    } else {
                        writer.println("NACK-Token invalid");
                    }
                }
                case "END" -> {
                    writer.println("ACK-Connection closed");
                    socket.close();
                    System.out.println("Player disconnected");
                    return;
                }
                default -> writer.println("NACK-Invalid Operation");
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            return;
        }

        try {
            int port = Integer.parseInt(args[0]);
            int mode = Integer.parseInt(args[1]);
            String filename = args.length == 3 ? args[2] : DATABASE_FILENAME;

            if (mode != 0 && mode != 1) {
                printUsage();
                return;
            }

            Server server = new Server(port, mode, filename);
            server.start();

        } catch (NumberFormatException e) {
            printUsage();
        } catch (IOException e) {
            System.out.println("I/O error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
