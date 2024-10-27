# Parallel and Distributed Computing

## Hangman Game

A client-server system using TCP sockets in Java. There are users that can authenticate with the system in order to play some text based game among them. A game is handled by a class and requires a given number of connected players to start. The implementation tolerate broken connections when users are queuing and waiting for the game to start. A protocol was developed between client and server that allows the clients to not lose their position in the game wait queue when resuming broken connections. Server implemented proprieties:

- Fault Tolerance
- No race conditions
- Minimize thread overheads
- Avoid slow clients

### How to run Server

* Open terminal and run `./compile.sh`, on the src directory

* After run the command for normal gameplay:

```
java -Djavax.net.ssl.keyStore=serverkeystore.jks     -Djavax.net.ssl.keyStorePassword=password     -Djavax.net.ssl.trustStore=clienttruststore.jks     -Djavax.net.ssl.trustStorePassword=password -cp ".:./lib/gson-2.10.1.jar:./lib/jbcrypt-0.4.jar" Server 1234 0 database.json
```

* And for ranked mode:

```
java -Djavax.net.ssl.keyStore=serverkeystore.jks     -Djavax.net.ssl.keyStorePassword=password     -Djavax.net.ssl.trustStore=clienttruststore.jks     -Djavax.net.ssl.trustStorePassword=password -cp ".:./lib/gson-2.10.1.jar:./lib/jbcrypt-0.4.jar" Server 1234 1 database.json
```

### How to run clients:

* After compiling run command below for each client, (username:2 password:2, username:3 password:3, username:4 password:4):

```
java -Djavax.net.ssl.keyStore=serverkeystore.jks     -Djavax.net.ssl.keyStorePassword=password     -Djavax.net.ssl.trustStore=clienttruststore.jks     -Djavax.net.ssl.trustStorePassword=password  -cp ".:./lib/gson-2.10.1.jar:./lib/jbcrypt-0.4.jar" Client 1234 0
```

### How to play

The game itself is pretty straightforward, similar to the classic Hangman game, each player tries to guess a letter of a random generated word and gets points for correct guesses.
