package chandy;

import java.io.*;
import java.net.*;
import java.util.*;

public class ProcessNode {
    private static final int RAND_MAX = 32767;

    private final int id, port, N;
    private int[] f;
    private final Map<Integer, Queue<Integer>> queue = new HashMap<>();
    private boolean hasToken = false, inCS = false;
    private int mCounter = 0;
    private int sentCount = 0, recvCount = 0;
    private final List<InetSocketAddress> peers = new ArrayList<>();
    private final String logFileName;

    public ProcessNode(int id, String peersConfig, int port) throws IOException {
        this.id = id; this.port = port;
        String[] parts = peersConfig.split(",");
        N = parts.length + 1;
        f = new int[N];
        for (int j = 0; j < N; j++) queue.put(j, new LinkedList<>());
        for (String p: parts) {
            String[] hp = p.split(":");
            peers.add(new InetSocketAddress(hp[0], Integer.parseInt(hp[1])));
        }
        if (id == 0) hasToken = true;
        String hostname = InetAddress.getLocalHost().getHostName();
        logFileName = hostname + "-cs.log";
        new FileWriter(logFileName, true).close();
    }

    public void start() {
        new Thread(this::runServer).start();
        System.out.printf("[p%d] Started on port %d, peers=%s%n", id, port, peers);
    }

    private void runServer() {
        try (ServerSocket server = new ServerSocket(port)) {
            while (true) {
                Socket sock = server.accept();
                new Thread(new MessageHandler(this, sock)).start();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public synchronized void requestCS() {
        mCounter++;
        System.out.printf("[p%d] REQUEST(%d)%n", id, mCounter);
        for (InetSocketAddress peer: peers) send(new Message(Message.Type.REQUEST, id, mCounter, null), peer);
        while (!hasToken) {
            try { wait(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        inCS = true;
        System.out.printf("[p%d] Entering CS%n", id);
        enterCriticalSection();
    }

    public synchronized void receiveRequest(int j, int mj) {
        recvCount++;
        System.out.printf("[p%d] Received REQUEST(%d) from p%d%n", id, mj, j);
        if (!hasToken) {
            queue.get(j).add(mj);
        } else if (!inCS) {
            hasToken = false;
            System.out.printf("[p%d] Passing TOKEN to p%d%n", id, j);
            send(new Message(Message.Type.TOKEN, id, 0, f), peerAddr(j));
        } else {
            queue.get(j).add(mj);
        }
    }

    public synchronized void receiveToken(int[] tokenF) {
        recvCount++;
        System.out.printf("[p%d] Received TOKEN%n", id);
        f = tokenF; hasToken = true; inCS = false;
        notifyAll();
    }

    public synchronized void exitCS() {
        System.out.printf("[p%d] Exiting CS%n", id);
        inCS = false; f[id] = mCounter;
        for (int j = 0; j < N; j++) {
            Queue<Integer> q = queue.get(j);
            if (!q.isEmpty() && q.peek() > f[j]) {
                q.poll();
                hasToken = false;
                System.out.printf("[p%d] Passing TOKEN to p%d%n", id, j);
                send(new Message(Message.Type.TOKEN, id, 0, f), peerAddr(j));
                break;
            }
        }
    }

    private void enterCriticalSection() {
        Random rnd = new Random();
        int randVal = rnd.nextInt(RAND_MAX + 1);
        double frac = (double) randVal / RAND_MAX;
        int loops = (int) (frac * 1_000_000);
        try (FileWriter fw = new FileWriter(logFileName, true)) {
            for (int i = 1; i < loops; i++) {
                fw.write(String.format("<%s,p%d,%d>%n",
                    InetAddress.getLocalHost().getHostName(), id, System.currentTimeMillis()));
            }
        } catch (IOException e) { e.printStackTrace(); }
        exitCS();
    }

    private InetSocketAddress peerAddr(int j) {
        // peers list excludes self, so adjust index
        return peers.get(j < id ? j : j - 1);
    }

    private void send(Message msg, InetSocketAddress dest) {
        try (Socket s = new Socket(dest.getHostName(), dest.getPort());
             ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream())) {
            oos.writeObject(msg);
            sentCount++;
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void printStats() {
        System.out.printf("[p%d] Sent=%d  Recv=%d%n", id, sentCount, recvCount);
    }
}
