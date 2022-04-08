import sockets.GMSocket;
import sockets.PMSocket;
import sockets.AuthenticationSocket;

public class Main {
    public static void main(String[] args) {
        AuthenticationSocket authenticationSocket = new AuthenticationSocket();
        authenticationSocket.run();

        PMSocket pmSocket = new PMSocket();

        GMSocket gmSocket = new GMSocket();
        gmSocket.run();
    }
}
