package sockets;

import Exceptions.MyServerException;
import database.Database;
import models.GroupMessage;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GMSocket {
    ServerSocket server;

    final byte[] serverAddress = {127, 0, 0, 1};
    final int serverPort = 50000;

    Database db = Database.getInstance();

    Map<String, Socket> onlineUsers = new ConcurrentHashMap<>();

    public void run() {
        Thread thread = new Thread(() -> {
            try {
                server = new ServerSocket();
                server.bind(new InetSocketAddress(InetAddress.getByAddress(serverAddress), serverPort));
                while (true) {
                    System.out.println("waiting for a connection request from a client in GMSocket...");
                    Socket client = server.accept();
                    Thread newThread = new Thread(() -> {
                        try {
                            DataInputStream in =
                                    new DataInputStream(new BufferedInputStream(client.getInputStream()));
                            DataOutputStream out =
                                    new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
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

    private void handleRequest(String message, Socket socket) throws MyServerException {
        String[] messageArray = message.split(" -Option ");
        String request = messageArray[0];
        if (request.matches("GM Connect")) {
            handleGroupConnect(socket, messageArray);
        } else if (request.matches("Group")) {
            handleGroupJoin(socket, messageArray);
        } else if (request.matches("End")) {
            handleLeaveGroup(socket, messageArray);
        } else if (request.matches("CreateGroup")) {
            handleCreateGroup(socket, messageArray);
        } else if (request.matches("Users")) {
            handleGetUsers(socket, messageArray);
        } else if (request.matches("GM GetAll")) {
            handleGMGetAll(socket, messageArray);
        } else if (request.matches("GM GetAllFrom")) {
            handleGMGetAllFrom(socket, messageArray);
        } else if (request.matches("GM")) {
            handleGM(socket, messageArray);
        } else {
            throw new MyServerException("Unknown Request");
        }
    }

    private void handleLeaveGroup(Socket socket, String[] messageArray) throws MyServerException {
        String sid = null;
        String groupId = null;
        for (int i = 1; i < messageArray.length; i++) {
            String[] option = messageArray[i].split("[<>:]");
            if (option[1].matches("SID")) sid = option[2];
            else if (option[1].matches("gname")) groupId = option[2];
        }

        if (sid == null || groupId == null) throw new MyServerException("Invalid \"End\" request format");
        String userId = validateUser(sid);
        if (userId == null) throw new MyServerException("Invalid SID");

        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            db.removeUserFromGroup(userId, groupId);
            String leaveMessage = String.format("%s left the %s chatroom", userId, groupId);
            sendGM(groupId, leaveMessage, leaveMessage.getBytes().length, null, null);
            out.writeUTF("LeftGroup");
            out.flush();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new MyServerException("Something went wrong");
        }
    }

    private void handleGroupConnect(Socket socket, String[] messageArray) throws MyServerException {
        String sid = null;
        for (int i = 1; i < messageArray.length; i++) {
            String[] option = messageArray[i].split("[<>:]");
            if (option[1].matches("SID")) sid = option[2];
        }

        if (sid == null) throw new MyServerException("Invalid \"Group\" request format");
        String userId = validateUser(sid);
        if (userId == null) throw new MyServerException("Invalid SID");
        onlineUsers.put(userId, socket);
    }

    private void handleGroupJoin(Socket socket, String[] messageArray) throws MyServerException {
        String sid = null;
        String groupId = null;
        for (int i = 1; i < messageArray.length; i++) {
            String[] option = messageArray[i].split("[<>:]");
            if (option[1].matches("SID")) sid = option[2];
            else if (option[1].matches("gname")) groupId = option[2];
        }

        if (sid == null || groupId == null) throw new MyServerException("Invalid \"Group\" request format");
        String userId = validateUser(sid);
        if (userId == null) throw new MyServerException("Invalid SID");

        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            joinGroup(groupId, userId);
            out.writeUTF("JoinedGroup");
            out.flush();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new MyServerException("Something went wrong");
        }
    }

    private void joinGroup(String groupId, String userId) throws MyServerException {
        db.addUserToGroup(groupId, userId);
        String joinMessage = String.format("%s joined the chat", userId);
        String welcomeMessage = String.format("Hi %s, welcome to the %s chat room.", userId, groupId);
        sendGM(groupId, joinMessage, joinMessage.getBytes().length, null, null);
        sendGM(groupId, welcomeMessage, welcomeMessage.getBytes().length, null, userId);
    }


    // handles CreateGroup request
    private void handleCreateGroup(Socket socket, String[] messageArray) throws MyServerException {
        String sid = null;
        String groupId = null;
        for (int i = 1; i < messageArray.length; i++) {
            String[] option = messageArray[i].split("[<>:]");
            if (option[1].matches("SID")) sid = option[2];
            else if (option[1].matches("gname")) groupId = option[2];
        }

        if (sid == null || groupId == null) throw new MyServerException("Invalid \"CreateGroup\" request format");
        String userId = validateUser(sid);
        if (userId == null) throw new MyServerException("Invalid SID");

        db.insertNewGroup(groupId, userId);
        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            out.writeUTF("GroupCreated");
            out.flush();
            socket.close();
            joinGroup(groupId, userId);
        } catch (IOException e) {
            e.printStackTrace();
            throw new MyServerException("Something went wrong");
        }
    }

    private void handleGetUsers(Socket socket, String[] messageArray) throws MyServerException {
        String groupId = null;
        for (int i = 1; i < messageArray.length; i++) {
            String[] option = messageArray[i].split("[<>:]");
            if (option[1].matches("gname")) groupId = option[2];
        }

        if (groupId == null) throw new MyServerException("Invalid \"Users\" request format");

        ArrayList<String> users = getGroupUsers(groupId);

        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            String response = "USERS_LIST:\r\n";
            for (int i = 0; i < users.size(); i++) {
                if (i != 0) response = response.concat("|");
                response = response.concat(String.format("<%s>", users.get(i)));
            }
            out.writeUTF(response);
            out.flush();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new MyServerException("Something went wrong");
        }

    }

    private void handleGM(Socket socket, String[] messageArray) throws MyServerException {
        String sid = null;
        String groupId = null;
        String lengthString = null;
        String message = null;
        for (int i = 1; i < messageArray.length; i++) {
            String[] option = messageArray[i].split("[<>:]");
            if (option[1].matches("SID")) sid = option[2];
            else if (option[1].matches("to")) groupId = option[2];
            else if (option[1].matches("message_len")) lengthString = option[2];
            else if (option[1].matches("message_body")) message = option[2];
        }

        if (sid == null || groupId == null || lengthString == null || message == null)
            throw new MyServerException("Invalid \"GM\" request format");

        int length;
        try {
            length = Integer.parseInt(lengthString);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            throw new MyServerException("message length should be a number");
        }

        try {
            sendGM(groupId, message, length, sid, null);
            DataOutputStream senderOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            senderOut.writeUTF("SENT GM");
            senderOut.flush();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new MyServerException("Something went wrong");
        }
    }

    private void handleGMGetAll(Socket socket, String[] messageArray) throws MyServerException {
        String sid = null;
        for (int i = 1; i < messageArray.length; i++) {
            String[] option = messageArray[i].split("[<>:]");
            if (option[1].matches("SID")) sid = option[2];
        }

        if (sid == null) throw new MyServerException("Invalid \"GM GetAll\" request format");
        String userId = validateUser(sid);
        if (userId == null) throw new MyServerException("Invalid SID");

        ArrayList<GroupMessage> messages = db.getAllGroupMessages(userId);
        sendGroupMessages(socket, messages);
    }

    private void handleGMGetAllFrom(Socket socket, String[] messageArray) throws MyServerException {
        String sid = null;
        String groupId = null;
        for (int i = 1; i < messageArray.length; i++) {
            String[] option = messageArray[i].split("[<>:]");
            if (option[1].matches("SID")) sid = option[2];
            else if (option[1].matches("gname")) groupId = option[2];
        }

        if (sid == null || groupId == null) throw new MyServerException("Invalid \"GM GetAllFrom\" request format");
        String userId = validateUser(sid);
        if (userId == null) throw new MyServerException("Invalid SID");

        ArrayList<GroupMessage> messages = db.getAllGroupMessagesFrom(userId, groupId);
        sendGroupMessages(socket, messages);
    }

    private void sendGroupMessages(Socket socket, ArrayList<GroupMessage> messages) throws MyServerException {
        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            String response = "GM All\r\n";
            for (GroupMessage gm : messages) {
                response = response.concat(String.format(
                        "GM -Option <from:%s> -Option <to:%s> -Option <message_len:%d> " +
                                "-Option <message_body:%s> -Option <send_time:%s>\r\n",
                        gm.getSenderId(),
                        gm.getGroupId(),
                        gm.getMessage().getBytes().length,
                        gm.getMessage(),
                        gm.getSendTime()
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

    private void sendGM(
            String groupId,
            String message,
            int length,
            String sid,
            String receiverUsername
    ) throws MyServerException {
        try {
            String senderUsername;
            senderUsername = validateUser(sid);
            Timestamp sendTime = new Timestamp(System.currentTimeMillis());
            db.saveGM(groupId, senderUsername, receiverUsername, message);
            System.out.printf("a group message sent from %s to %s: \"%s\"\n", senderUsername, groupId, message);
            sendGMToOnlineUsers(groupId, senderUsername, receiverUsername, message, length, sendTime);
            sendGMToOnlineUsers(groupId, senderUsername, receiverUsername, message, length, sendTime);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendGMToOnlineUsers(
            String groupId,
            String senderUsername,
            String receiverUsername,
            String message,
            int messageLength,
            Timestamp sendTime
    ) throws IOException {
        try {
            ArrayList<String> users = getGroupUsers(groupId);
            if (receiverUsername != null) {
                if (!users.contains(receiverUsername)) throw new MyServerException("receiver user does not exist");
                sendGMToAnOnlineUser(groupId, senderUsername, receiverUsername, message, messageLength, sendTime);
            } else {
                for (String username : users) {
                    sendGMToAnOnlineUser(groupId, senderUsername, username, message, messageLength, sendTime);
                }
            }
        } catch (MyServerException e) {
            e.printStackTrace();
        }
    }

    private void sendGMToAnOnlineUser(
            String groupId,
            String senderUsername,
            String receiverUsername,
            String message,
            int messageLength,
            Timestamp sendTime
    ) throws IOException {
        if (onlineUsers.containsKey(receiverUsername)) {
            Socket receiverSocket = onlineUsers.get(receiverUsername);
            DataOutputStream out =
                    new DataOutputStream(new BufferedOutputStream(receiverSocket.getOutputStream()));
            out.writeUTF(
                    String.format(
                            "GM -Option <from:%s> -Option <to:%s> -Option <message_len:%d> " +
                                    "-Option <message_body:%s> -Option <send_time:%s>",
                            senderUsername,
                            groupId,
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

    private ArrayList<String> getGroupUsers(String groupId) throws MyServerException {
        return db.getGroupUsers(groupId);
    }

    private String validateUser(String sid) throws MyServerException {
        return db.validate(sid);
    }
}
