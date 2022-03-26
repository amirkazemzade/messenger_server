import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class UserSocket {

    // address and port of Receiver Socket
    byte[] serverAddress = {127, 0, 0, 1};
    int serverPort = 40001;

    void createUser(String username, String password, Socket socket) {
        Database db = Database.getInstance();

        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            int id;
            try {
                id = db.insertUser(username, password);
            } catch (Exception e) {
                out.writeUTF(String.format("User Not Accepted -Option <reason:\"%s\">", e.getMessage()));
                out.flush();
                socket.close();
                e.printStackTrace();
                return;
            }
            if (id >= 1) {
                out.writeUTF(String.format("User Accepted -Option <id:%d>", id));
            } else {
                out.writeUTF(String.format("User Not Accepted -Option <reason:\"%s\">", "Something went wrong!"));
            }
            out.flush();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void loginUser(String username, String password, Socket socket) {
        Database db = Database.getInstance();

        //todo add session id
        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            int id;
            try {
                id = db.login(username, password);
            } catch (Exception e) {
                out.writeUTF(String.format("ERROR -Option <reason:%s>", e.getMessage()));
                out.flush();
                socket.close();
                e.printStackTrace();
                return;
            }
            if (id >= 1) {
                out.writeUTF(String.format("Connected -Option <id:%d>", id));
            } else {
                out.writeUTF(String.format("ERROR -Option <reason:%s>", "Something went wrong!"));
            }
            out.flush();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // function that starts socket
//    void run() {
//        System.out.println("starting Receiver Socket...");
//        Thread thread = new Thread(() -> {
//            try {
//                // creating Receiver Socket
//                ServerSocket server = new ServerSocket();
//                server.bind(new InetSocketAddress(InetAddress.getByAddress(serverAddress), serverPort));
//                System.out.println("Receiver Socket has started");
//                while (true) {
//                    try {
//                        System.out.println("waiting for a connection request from a client");
//                        // accepting client connect request
//                        Socket socket = server.accept();
//
//                        // creating input stream for receiving data
//                        DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
//
//                        String message = in.readUTF();
//                        handleRequest(message, socket);
//
//                    } catch (Exception e) {
//                        System.out.println("an exception happen during connection with a client: ");
//                    }
//                }
//            } catch (Exception e) {
//                System.out.println("an exception happen while trying to run server: ");
//                e.printStackTrace();
//            }
//        });
//        thread.start();
//
//    }
}
