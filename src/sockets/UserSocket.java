package sockets;

import Exceptions.MyServerException;
import database.Database;
import models.Session;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.net.Socket;


public class UserSocket {

    Database db = Database.getInstance();

    void createUser(String username, String password, Socket socket) {
        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            String id;
            try {
                id = db.insertUser(username, password);
            } catch (MyServerException e) {
                out.writeUTF(String.format("User Not Accepted -Option <reason:\"%s\">", e.getMessage()));
                out.flush();
                socket.close();
                e.printStackTrace();
                return;
            }
            out.writeUTF(String.format("User Accepted -Option <id:%s>", id));
            out.flush();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void loginUser(String username, String password, Socket socket) {
        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            Session session;
            try {
                session = db.login(username, password);
            } catch (MyServerException e) {
                out.writeUTF(String.format("ERROR -Option <reason:%s>", e.getMessage()));
                out.flush();
                socket.close();
                e.printStackTrace();
                return;
            }
            out.writeUTF(
                    String.format(
                            "Connected -Option <id:%s> -Option <SID:%s>",
                            session.getUsername(),
                            session.getsid()
                    )
            );
            out.flush();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
