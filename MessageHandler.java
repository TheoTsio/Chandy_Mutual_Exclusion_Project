package chandy;

import java.io.ObjectInputStream;
import java.net.Socket;

public class MessageHandler implements Runnable {
    private final ProcessNode node;
    private final Socket sock;

    public MessageHandler(ProcessNode node, Socket sock) {
        this.node = node; this.sock = sock;
    }

    @Override
    public void run() {
        try (ObjectInputStream ois = new ObjectInputStream(sock.getInputStream())) {
            Message msg = (Message) ois.readObject();
            switch (msg.type) {
                case REQUEST -> node.receiveRequest(msg.senderId, msg.m);
                case TOKEN   -> node.receiveToken(msg.f);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
