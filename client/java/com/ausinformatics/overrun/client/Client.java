package com.ausinformatics.overrun.client;

import java.util.Set;

import org.reflections.Reflections;

class Client {
    private static ClientLibrary library;

    public static void main(String[] args) {
        library = new ClientLibrary();
        library.printVersion();
        String server = null;
        int port = 12317;
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                if (args[i].contains("h")) {
                    String myName = Thread.currentThread().getStackTrace()[2].getClassName();
                    library.printHelp(myName);
                }
                if (args[i].contains("v")) {
                    library.printVersion();
                }
                if (args[i].contains("e")) {
                    ClientLibrary.echo_mode = true;
                }
            } else if (server == null) {
                server = args[i];
            } else {
                try {
                    port = Integer.parseInt(args[i]);
                } catch (Exception e) {
                    System.err.println("Must provide a valid integer for the port\n");
                    return;
                }
            }
        }

        if (server == null) {
            System.err.println("Must provide a server address\n");
            return;
        }

        /* Find the class that implements ClientInterface */
        Reflections reflections = new Reflections("com.ausinformatics.overrun.client");
        Set<Class<? extends ClientInterface>> classes = reflections.getSubTypesOf(ClientInterface.class);
        ClientInterface client = null;
        for (Class<? extends ClientInterface> cls : classes) {
            try {
                client = (ClientInterface) cls.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                System.err.println("Couldn't find a class that implements ClientInterface; this is unrecoverable");
                return;
            }
        }

        library.registerClient(client);

        System.err.println("Configured!");
        System.err.printf("Trying to connect to %s, port %d\n", server, port);
        while (true) {
            while (true) {
                System.err.println("Attempting to connect...");
                library.connectServer(server, port);
                if (library.isSocketGood())
                    break;
                System.err.println("    failed to connect. Will retry...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
            System.err.println("    connected!");
            library.mainLoop();
            System.err.println("Was disconnected\n");
            library.disconnectServer();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
