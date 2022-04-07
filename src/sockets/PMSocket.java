package sockets;

import Exceptions.MyServerException;
import database.Database;
import models.PrivateMessage;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PMSocket {

    ServerSocket server;

    final byte[] serverAddress = {127, 0, 0, 1};
    final int serverPort = 41000;

    Database db = Database.getInstance();

    Map<String, Socket> onlineUsers = new ConcurrentHashMap<>();

    public PMSocket() {
        run();
    }

    void run() {
        Thread thread = new Thread(() -> {
            try {
                server = new ServerSocket();
                server.bind(new InetSocketAddress(InetAddress.getByAddress(serverAddress), serverPort));
                while (true) {
                    System.out.println("waiting for a connection request from a client in PMSocket...");
                    Socket client = server.accept();
                    Thread newThread = new Thread(() -> {
                        try {
                            DataInputStream in = new DataInputStream(new BufferedInputStream(client.getInputStream()));
                            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
                            try {
                                String message = in.readUTF();
                                handleRequest(message, client);
                            } catch (MyServerException e) {
                                e.printStackTrace();
                                out.writeUTF(String.format("ERROR -Option <reason:%s>", e.getMessage()));
                                out.flush();
                                client.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    newThread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    // determines request from message and calls appropriate socket to handle it
    private void handleRequest(String message, Socket socket) throws MyServerException {
        String[] messageArray = message.split(" -Option ");
        String request = messageArray[0];
        if (request.matches("PM Connect")) {
            handleConnect(socket, messageArray);
        } else if (request.matches("PM ConnectTo")) {
            handlePMConnectTo(socket, messageArray);
        } else if (request.matches("PM GetAll")) {
            handlePMGetAll(socket, messageArray);
        } else if (request.matches("PM GetAllFrom")) {
            handlePMGetAllFrom(socket, messageArray);
        } else if (request.matches("PM")) {
            handlePM(socket, messageArray);
        } else {
            throw new MyServerException("Unknown Request");
        }
    }

    private void handlePMGetAllFrom(Socket socket, String[] messageArray) throws MyServerException {
        String sid = null;
        String wantedUser = null;
        for (int i = 1; i < messageArray.length; i++) {
            String[] option = messageArray[i].split("[<>:]");
            if (option[1].matches("SID")) sid = option[2];
            else if (option[1].matches("from")) wantedUser = option[2];
        }

        if (sid == null || wantedUser == null) throw new MyServerException("Invalid \"PM GetAllFrom\" format");
        String requestedUser = validateUser(sid);
        ArrayList<PrivateMessage> messages = db.getAllPrivateMessages(requestedUser, wantedUser);
        sendAllMessages(socket, messages);
    }

    private void sendAllMessages(Socket socket, ArrayList<PrivateMessage> messages) throws MyServerException {
        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            String response = "PM All\r\n";
            for (PrivateMessage pm : messages) {
                response = response.concat(String.format(
                        "PM -Option <from:%s> -Option <to:%s> -Option <message_len:%d> " +
                                "-Option <message_body:%s> -Option <send_time:%s>\r\n",
                        pm.getSenderId(),
                        pm.getReceiverId(),
                        pm.getMessageLength(),
                        pm.getMessage(),
                        pm.getSendTime()
                ));
            }
            out.writeUTF(response);
            out.flush();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new MyServerException("Something went wrong");
        }
    }

    private void handlePMGetAll(Socket socket, String[] messageArray) throws MyServerException {
        String sid = extractSID(messageArray);
        String senderUsername = validateUser(sid);
        ArrayList<PrivateMessage> messages = db.getAllPrivateMessages(senderUsername);
        sendAllMessages(socket, messages);
    }

    private void handlePMConnectTo(Socket socket, String[] messageArray) throws MyServerException {
        String sid = null;
        String receiverUsername = null;
        for (int i = 1; i < messageArray.length; i++) {
            String[] option = messageArray[i].split("[<>:]");
            if (option[1].matches("SID")) sid = option[2];
            if (option[1].matches("to")) receiverUsername = option[2];
        }
        if (sid == null || receiverUsername == null) {
            throw new MyServerException("Invalid \"PM ConnectTo\" format");
        }

        String senderUsername = validateUser(sid);

        String initialMessage = String.format("hey %s, %s wants to talk to you.", receiverUsername, senderUsername);

        sendPM(receiverUsername, initialMessage, initialMessage.length(), sid, socket);
    }

    private void handlePM(Socket socket, String[] messageArray) throws MyServerException {
        String sid = null;
        String receiverUsername = null;
        String message = null;
        String messageLengthString = null;
        for (int i = 1; i < messageArray.length; i++) {
            String[] option = messageArray[i].split("[<>:]");
            if (option[1].matches("SID")) sid = option[2];
            if (option[1].matches("to")) receiverUsername = option[2];
            if (option[1].matches("message_len")) messageLengthString = option[2];
            if (option[1].matches("message_body")) message = option[2];
        }

        if (sid == null || receiverUsername == null || message == null || messageLengthString == null) {
            throw new MyServerException("Invalid \"PM\" format");
        }

        int messageLength;

        try {
            messageLength = Integer.parseInt(messageLengthString);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            throw new MyServerException("message length should be a number");
        }

        sendPM(receiverUsername, message, messageLength, sid, socket);
    }

    private void handleConnect(Socket socket, String[] messageArray) throws MyServerException {
        String sid = extractSID(messageArray);
        if (sid == null) throw new MyServerException("Invalid \"PM Connect\" format");
        String userId = validateUser(sid);
        if (userId == null) throw new MyServerException("Invalid SID");
        onlineUsers.put(userId, socket);
    }

    private String extractSID(String[] messageArray) {
        String sid = null;
        for (int i = 1; i < messageArray.length; i++) {
            String[] option = messageArray[i].split("[<>:]");
            if (option[1].matches("SID")) sid = option[2];
        }
        return sid;
    }

    public void sendPM(String receiverUsername, String message, int messageLength, String sid, Socket senderSocket) {
        try {
            DataOutputStream senderOut = new DataOutputStream(new BufferedOutputStream(senderSocket.getOutputStream()));
            String senderUsername;
            try {
                if (!doesUserExist(receiverUsername))
                    throw new MyServerException("receiver user does not exist");
                senderUsername = validateUser(sid);
                Timestamp sendTime = new Timestamp(System.currentTimeMillis());
                db.savePM(senderUsername, receiverUsername, message);
                sendPMToOnlineUser(receiverUsername, senderUsername, receiverUsername, message, messageLength, sendTime);
                sendPMToOnlineUser(senderUsername, senderUsername, receiverUsername, message, messageLength, sendTime);
                senderOut.writeUTF("SENT PM");
                senderOut.flush();
                senderSocket.close();
            } catch (MyServerException e) {
                senderOut.writeUTF(String.format("ERROR -Option <reason:%s>", e.getMessage()));
                senderOut.flush();
                senderSocket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendPMToOnlineUser(String deliverTo, String senderUsername, String receiverUsername, String message, int messageLength, Timestamp sendTime) throws IOException {
        if (onlineUsers.containsKey(deliverTo)) {
            Socket receiverSocket = onlineUsers.get(deliverTo);
            DataOutputStream out =
                    new DataOutputStream(new BufferedOutputStream(receiverSocket.getOutputStream()));
            out.writeUTF(
                    String.format(
                            "PM -Option <from:%s> -Option <to:%s> -Option <message_len:%d> " +
                                    "-Option <message_body:%s> -Option <send_time:%s>",
                            senderUsername,
                            receiverUsername,
                            messageLength,
                            message,
                            sendTime.getTime()
                    )
            );
            out.flush();
            receiverSocket.close();
            onlineUsers.remove(receiverUsername);
        }
    }

    private String validateUser(String sid) throws MyServerException {
        return db.validate(sid);
    }

    private boolean doesUserExist(String username) throws MyServerException {
        return db.doesUserExist(username);
    }
}
