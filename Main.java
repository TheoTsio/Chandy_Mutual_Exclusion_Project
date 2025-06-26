package chandy;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: java chandy.Main <id> <port> <peersConfig>");
            System.exit(1);
        }
        int id     = Integer.parseInt(args[0]);
        int port   = Integer.parseInt(args[1]);
        String peersConfig = args[2]; // "host:port,host:port,…"

        ProcessNode node = new ProcessNode(id, peersConfig, port);
        node.start();

        Scanner sc = new Scanner(System.in);
        System.out.println("Type 'request' for request critical section, 'exit' for exit critical section, 'stats' for measurements");
        while (sc.hasNextLine()) {
            String cmd = sc.nextLine().trim().toLowerCase();
            switch (cmd) {
                case "request":
                    node.requestCS();
                    break;
                case "exit":
                    node.exitCS();
                    break;
                case "stats":
                    node.printStats();
                    break;
                default:
                    System.out.println("Άγνωστη εντολή. Δοκίμασε request|exit|stats.");
            }
        }
        sc.close();
    }
}
