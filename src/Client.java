import javax.net.SocketFactory;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;


public class Client {
    private final String hostname;
    private final int port;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private static final String HOSTNAME = "localhost";
    private static final int RECEIVE_TIMEOUT_MS = 10000;

    public Client(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    private static void printUsage() {
        System.out.println("Usage: Client <PORT>  [HOSTNAME]");
        System.out.println("where:");
        System.out.println("<PORT> is an integer");
        System.out.println("[HOSTNAME] is the hostname");
    }


    private void connectToServer() throws IOException {
        SocketFactory factory = SSLSocketFactory.getDefault();

        this.socket = factory.createSocket(hostname, port);
        
        /*SSLParameters sslParams = new SSLParameters();
        sslParams.setEndpointIdentificationAlgorithm("HTTPS");
        ((SSLSocket) socket).setSSLParameters(sslParams);*/

        this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        this.writer = new PrintWriter(this.socket.getOutputStream(), true);
        System.out.println("Connected to the server at " + this.hostname + ":" + this.port);
    }

    private void send(String message) {
        writer.println(message);
    }

    private String receive() throws IOException {
        this.socket.setSoTimeout(RECEIVE_TIMEOUT_MS); // Set socket timeout
        try {
            String response = reader.readLine(); // Read with timeout
            if (response == null) {
                throw new IOException("Connection to server lost.");
            }
            return response;
        } catch (SocketTimeoutException e) {
            System.out.println("Server did not respond within the timeout period. Please try again.");
            return null; // Or handle the timeout in a way suitable for your application.
        } finally {
            this.socket.setSoTimeout(0); // Reset timeout to infinite
        }
    }


    private void register(Scanner scanner) {
        System.out.println("REGISTRATION");
        try {
            String code;
            do {
                scanner.nextLine();
                System.out.println("Enter username:");
                String username = scanner.nextLine();
                System.out.println("Enter password:");
                String password = scanner.nextLine();

                send("REG-" + username + " " + password);
                String[] serverResponse = receive().split("-", 2);
                code = serverResponse[0];
                System.out.println(serverResponse[1]);
            } while (!code.equals("ACK"));

            this.send("END-Closing connection");
            System.out.println(receive().split("-", 2)[1]);

        } catch (IOException e) {
            System.out.println("Error during registration: " + e.getMessage());
        }
    }

    private boolean auth(Scanner scanner) {
        System.out.println("AUTHENTICATION");
        try {
            do {
                scanner.nextLine();
                System.out.println("1 - Authentication or 2 - Reconnection? (Type 1 or 2)");
                String answer = scanner.nextLine();
                if (answer.equals("2")) {
                    System.out.println("Give your token?");
                    String token = scanner.nextLine();
                    System.out.println("Token: " + token);
                    this.send("TOKEN-" + token);
                    String received = this.receive();
                    System.out.println("Received: " + received);
                    String[] serverResponse;
                    if (received != null) {
                        serverResponse = received.split("-", 2);
                    } else {
                        System.out.println("Server did not respond within the timeout period. Please try again.");
                        continue;
                    }
                    System.out.println("After receive");
                    String code = serverResponse[0];
                    System.out.println(serverResponse[1]);

                    if (code.equals("QUEUE")) {
                        return true;
                    } else {
                        System.out.println("Let's try again!");
                    }
                } else break;
            } while (true);

            String code;
            do {
                System.out.println("Enter username:");
                String username = scanner.nextLine();
                System.out.println("Enter password:");
                String password = scanner.nextLine();

                this.send("AUTH-" + username + " " + password);
                String[] serverResponse = this.receive().split("-", 2);
                code = serverResponse[0];
                if (code.equals("TOKEN")) {
                    System.out.println("Token: " + serverResponse[1]);
                    return true;
                } else {
                    System.out.println(serverResponse[1]);
                    System.out.println("Do you want to try again? (Type yes or no)");
                    String answer = scanner.nextLine().toLowerCase();
                    if (!answer.equals("yes")) break;
                }
            } while (true);

            return false;
        } catch (IOException e) {
            System.out.println("Error during authentication: " + e.getMessage());
            return false;
        }
    }

    public void playGame() {
        GameClient gameClient = new GameClient(this.reader, this.writer);
        gameClient.play();
    }

    public void closeConnection() {
        try {
            if (this.socket != null) {
                this.socket.close();
            }
            if (this.reader != null) {
                this.reader.close();
            }
            if (this.writer != null) {
                this.writer.close();
            }

        } catch (IOException e) {
            System.out.println("Error when closing the connection: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        try {
            int port = Integer.parseInt(args[0]);
            String hostname = args.length == 2 ? args[1] : HOSTNAME;

            Client client = new Client(hostname, port);
            client.connectToServer();

            Scanner scanner = new Scanner(System.in);
            int mode;

            do {
                System.out.println("\nChoose a mode:");
                System.out.println("0 - Register");
                System.out.println("1 - Authenticate and Play");
                System.out.print("Enter your choice: ");
                while (!scanner.hasNextInt()) {
                    System.out.println("Invalid input. Please enter a number.");
                    scanner.next(); // Discard invalid input
                }
                mode = scanner.nextInt();
            } while (mode != 0 && mode != 1);

            if (mode == 0) {
                client.register(scanner);
            } else {
                if (client.auth(scanner)) {
                    client.playGame();
                }
            }

            client.closeConnection();
        } catch (NumberFormatException e) {
            printUsage();
        } catch (UnknownHostException e) {
            System.out.println("Server not found: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("I/O error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
