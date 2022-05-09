package sockets;

import Exceptions.MyServerException;
import models.User;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

// handling request sent to server and calling the appropriate class to send response
public class AuthenticationSocket {

    // address and port of Receiver Socket
    private final byte[] serverAddress = {127, 0, 0, 1};
    private final int serverPort = 40000;

    // function that starts socket
    public void run() {
        System.out.println("starting Receiver Socket...");
        Thread thread = new Thread(() -> {
            try {
                // creating Receiver Socket
                ServerSocket server = new ServerSocket();
                server.bind(new InetSocketAddress(InetAddress.getByAddress(serverAddress), serverPort));
                System.out.println("Receiver Socket has started");
                while (true) {
                    Socket socket = server.accept();
                    Thread newThread = new Thread(() -> {
                        try {
                            System.out.println("waiting for a connection request from a client");
                            // accepting client connect request

                            // creating input stream for receiving data
                            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                            // handling request
                            try {
                                String message = in.readUTF();
                                handleRequest(message, socket);
                            } catch (MyServerException e) {
                                e.printStackTrace();
                                DataOutputStream out = new DataOutputStream(
                                        new BufferedOutputStream(
                                                socket.getOutputStream()
                                        )
                                );
                                out.writeUTF(e.getMessage());
                                out.flush();
                                socket.close();
                            }
                        } catch (Exception e) {
                            System.out.println("an exception happen during connection with a client: ");
                            e.printStackTrace();
                        }
                    });
                    newThread.start();
                }
            } catch (Exception e) {
                System.out.println("an exception happen while trying to run server: ");
                e.printStackTrace();
            }
        });
        thread.start();
    }

    // determines request from message and calls appropriate socket to handle it
    private void handleRequest(String message, Socket socket) throws MyServerException {
        String[] messageArray = message.split(" -Option ");
        String request = messageArray[0];
        if (request.matches("Make")) {
            handleMake(socket, messageArray);
        } else if (request.matches("Connect")) {
            handleConnect(socket, messageArray);
        } else {
            throw new MyServerException("Unknown Request");
        }
    }

    // handles make request
    private void handleMake(Socket socket, String[] messageArray) throws MyServerException {
        User user = extractUser(messageArray);
        if (user.getUsername() == null || user.getPassword() == null)
            throw new MyServerException("Invalid \"Make\" Request Format");
        UserSocket userSocket = new UserSocket();
        userSocket.createUser(user.getUsername(), user.getPassword(), socket);
    }

    // handles connect request
    private void handleConnect(Socket socket, String[] messageArray) throws MyServerException {
        User user = extractUser(messageArray);
        if (user.getUsername() == null || user.getPassword() == null)
            throw new MyServerException("Invalid \"Connect\" Request Format");
        UserSocket userSocket = new UserSocket();
        userSocket.loginUser(user.getUsername(), user.getPassword(), socket);
    }

    // extracts user information from received values
    private User extractUser(String[] messageArray) {
        String username = null;
        String password = null;
        for (int i = 1; i < messageArray.length; i++) {
            String[] option = messageArray[i].split("[<>:]");
            if (option[1].matches("user")) username = option[2];
            else if (option[1].matches("pass")) password = option[2];
        }
        return new User(username, password);
    }
}
