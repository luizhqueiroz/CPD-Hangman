import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GameClient {
    //private final Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public GameClient(BufferedReader reader, PrintWriter writer) {
        this.reader = reader;
        this.writer = writer;
    }

    private void send(String message) {
        this.writer.println(message);
    }

    private String receive() throws IOException {
        return this.reader.readLine();
    }

    public void play() {
        try {
            Scanner scanner = new Scanner(System.in);
            boolean end = false;
            Thread inputThread = null;
            Lock scannerLock = new ReentrantLock();
            final String[] answer = {""};
            do {
                String[] serverResponse = this.receive().split("-", 2);
                String code = serverResponse[0];
                switch (code) {
                    case "QUEUE" -> System.out.println(serverResponse[1]);
                    case "PING" -> {
                        System.out.println(serverResponse[1]);
                        this.send("ACK-Ready to play");
                    }
                    case "START" -> {
                        System.out.println(serverResponse[1]);
                        this.send("ACK-Start playing");
                    }
                    case "INFO" -> {
                        System.out.println(serverResponse[1]);
                        this.send("ACK-Information received");
                        if (serverResponse[1].equals("timeout") && inputThread != null) {
                            inputThread.interrupt();
                            inputThread = null;
                        }
                    }
                    case "GUESS" -> {
                        System.out.println(serverResponse[1]);
                        inputThread = new Thread(() -> {
                            scannerLock.lock();
                            try {
                                while (answer[0].isEmpty()) {
                                    System.out.println("Please enter a non-empty response:");
                                    answer[0] = scanner.nextLine().trim();
                                }
                                this.send("ACK-" + answer[0]);
                                answer[0] = "";
                            } finally {
                                scannerLock.unlock();
                            }
                        });
                        inputThread.start();
                    }
                    case "GAMEOVER" -> {
                        System.out.println(serverResponse[1]);
                        try {
                            while (answer[0].isEmpty()) {
                                System.out.println("Please enter a non-empty response:");
                                answer[0] = scanner.nextLine().trim();
                            }
                        } catch (IndexOutOfBoundsException e) {
                            System.out.println("Error reading input: " + e.getMessage());
                            // Handle the error appropriately
                        }

                        this.send("ACK-" + answer[0]);
                        answer[0] = "";
                    }
                    case "END" -> {
                        System.out.println(serverResponse[1]);
                        end = true;
                    }
                    default -> System.out.println("Wrong message from the server");
                }

            } while (!end);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
