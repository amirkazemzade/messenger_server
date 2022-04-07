import sockets.PMSocket;
import sockets.ReceiverSocket;

public class Main {
    public static void main(String[] args) {
        ReceiverSocket receiverSocket = new ReceiverSocket();
        receiverSocket.run();

        PMSocket pmSocket = new PMSocket();
    }
}
