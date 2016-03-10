package de.fhkl.imst.i.cgma.raytracer;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class Connection extends Thread {

    String myIP = null;
    ServerSocket serverSocket = null;
    List<ServerThread> serverThreadList = new ArrayList<ServerThread>();
    String recieve;
    ServerThread serverThread;
    boolean client;
    int xp;
    TCPSocket tcpSocket = null;

    public Connection() throws Exception {
        start();
    }

    @Override
    public void run() {
        List<String> reachableIPs = new ArrayList<String>();
        final int port = 1250;
        boolean existServer = false;

        try {
            reachableIPs = IPScanner(port);
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // testing if port is available
        Iterator<String> it = reachableIPs.iterator();
        System.out.println("Does a Server exist?");
        while (it.hasNext()) {
            String ip = it.next();
            try {
                // testing port
                tcpSocket = new TCPSocket(ip, port);
                System.out.println("Did find port " + port + " for IP adress: " + ip);
                tcpSocket.sendLine("Are you a Server?");
                String recieve = tcpSocket.receiveLine();
                if (recieve.equals("yes")) {
                    existServer = true;
                    System.out.println("Server exists.");
                }
                break;
            } catch (Exception e) {
                System.out.println("Didn't find port " + port + " on IP adress: " + ip);
                it.remove();
            }
        }

        if (existServer == true) {
            client = true;
            String recieve = "";
            System.out.println("Starting Client");
            try {
                tcpSocket.sendLine(myIP);
                Raytracer06.INSTANCE.setClient(client);
                Raytracer06.INSTANCE.serverOrClient();
                Boolean start = true;
                while (!recieve.equals("terminate")) {
                    recieve = tcpSocket.receiveLine();
                    if (Pattern.matches("\\d*", recieve) == true) {
                        xp = Integer.parseInt(recieve);
                        String send = Raytracer06.INSTANCE.doRayTrace(xp);
                        clientSendLine(send);
                    } else {
                        Raytracer06.INSTANCE.splitAndLoadDatString(recieve);
                        Raytracer06.INSTANCE.initDoRayTrace();
                    }
                }
            } catch (Exception e) {
                System.out.println("Client: " + e);
                e.printStackTrace();
            }

            if (tcpSocket != null) {
                try {
                    tcpSocket.close();
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        } else {
            client = false;
            System.out.println("Server doesn't exist.");

            // Starting Server

            System.out.println("Starting Server");
            Raytracer06.INSTANCE.howManyClients();
            Raytracer06.INSTANCE.setClient(client);
            Raytracer06.INSTANCE.serverOrClient();
            try {
                // create socket
                serverSocket = new ServerSocket(port);
            } catch (Exception e) {
                System.out.println("Fehler beim Erzeugen des ServerSockets");
                return;
            }
            while (true) {
                try {
                    System.out.println("Wating for connection");
                    tcpSocket = new TCPSocket(serverSocket.accept());
                    serverThread = new ServerThread(tcpSocket, recieve);
                    serverThreadList.add(serverThread);
                    Raytracer06.INSTANCE.howManyClients();
                } catch (Exception e) {
                    System.out.println("Server while loop: " + e);
                }
            }
        }
    }

    public void clientSendLine(String send) throws Exception {
        tcpSocket.sendLine(send);
    }

    // IPScanner
    public List<String> IPScanner(int port) throws Exception {

        String host = null;

        List<String> reachableIPs = new ArrayList<String>();

        // get subnet mask
        InetAddress localHost = Inet4Address.getLocalHost();
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localHost);
        int subnetmask = networkInterface.getInterfaceAddresses().get(1).getNetworkPrefixLength();

        // get own IP
        try {
            InetAddress ownIP = InetAddress.getLocalHost();
            myIP = ownIP.getHostAddress();
            System.out.println("IP of my system is: " + myIP);
        } catch (Exception e) {
            System.out.println("Exception caught =" + e.getMessage());
        }
        // Find IPs
        if (subnetmask == 24) {
            System.out.println("Subnetmask: " + subnetmask);
            String[] myIPsplit = myIP.split("[.]");
            String checkIP = myIPsplit[0] + "." + myIPsplit[1] + "." + myIPsplit[2] + ".";
            ConnectionThread curr = null;
            for (int i = 1; i < 255; i++) {
                host = checkIP + i;

                curr = new ConnectionThread(host, port, reachableIPs);
                Thread t = new Thread(curr);
                t.start();
                try {
                    t.sleep(1);
                } catch (InterruptedException e) {
                    System.out.println("Unable to sleep");
                }
                reachableIPs = curr.getReachableIPs();
            }
            try {
                Thread.sleep(curr.getTimeout());
            } catch (InterruptedException e) {
                System.out.println("Unable to Sleep: " + e);

            }

        } else if (subnetmask == 16) {
            System.out.println("subnetmask: " + subnetmask);
            String[] myIPsplit = myIP.split("[.]");
            String checkIP = myIPsplit[0] + "." + myIPsplit[1] + ".";
            ConnectionThread curr = null;
            for (int i = 0; i <= 255; i++) {
                for (int j = 1; j < 255; j++) {
                    host = checkIP + i + "." + j;

                    curr = new ConnectionThread(host, port, reachableIPs);
                    Thread t = new Thread(curr);
                    t.start();
                    try {
                        t.sleep(0);
                    } catch (InterruptedException e) {
                        System.out.println("Unable to sleep");
                    }
                    reachableIPs = curr.getReachableIPs();
                }
            }
            try {
                Thread.sleep(curr.getTimeout());
            } catch (InterruptedException e) {
                System.out.println("Unable to sleep");

            }
        } else if (subnetmask == 12) {
            System.out.println("subnetmask: " + subnetmask);
            String[] myIPsplit = myIP.split("[.]");
            String checkIP = myIPsplit[0] + ".";
            ConnectionThread curr = null;
            for (int i = 16; i <= 31; i++) {
                for (int j = 0; j <= 255; j++) {
                    for (int k = 1; k < 255; k++) {

                        host = checkIP + i + "." + j + "." + k;

                        curr = new ConnectionThread(host, port, reachableIPs);
                        Thread t = new Thread(curr);
                        t.start();
                        try {
                            t.sleep(0);
                        } catch (InterruptedException e) {
                            System.out.println("Unable to sleep");
                        }
                        reachableIPs = curr.getReachableIPs();
                    }
                }
            }
            try {
                Thread.sleep(curr.getTimeout());
            } catch (InterruptedException e) {
                System.out.println("unable to sleep");
            }
        } else if (subnetmask == 8) {
            System.out.println("subnetmask: " + subnetmask);
            String[] myIPsplit = myIP.split("[.]");
            String checkIP = myIPsplit[0] + ".";
            ConnectionThread curr = null;
            for (int i = 0; i <= 255; i++) {
                for (int j = 0; j <= 255; j++) {
                    for (int k = 1; k < 255; k++) {

                        host = checkIP + i + "." + j + "." + k;

                        curr = new ConnectionThread(host, port, reachableIPs);
                        Thread t = new Thread(curr);
                        t.start();
                        try {
                            t.sleep(0);
                        } catch (InterruptedException e) {
                            System.out.println("Unable to sleep");
                        }
                        reachableIPs = curr.getReachableIPs();
                    }
                }
            }
            try {
                Thread.sleep(curr.getTimeout());
            } catch (InterruptedException e) {
                System.out.println("Unable to sleep");
            }
        } else if (subnetmask == 21) {
            System.out.println("subnetmask: " + subnetmask);
            String[] myIPsplit = myIP.split("[.]");
            String checkIP = myIPsplit[0] + "." + myIPsplit[1] + ".";
            ConnectionThread curr = null;
            for (int i = 184; i <= 191; i++) {
                for (int j = 1; j < 255; j++) {

                    host = "10.0.1.1";//checkIP + i + "." + j;
                    if (host.equals(checkIP + 191 + "." + 254)) {
                        System.out.println("Last host:" + host);
                    }

                    curr = new ConnectionThread(host, port, reachableIPs);
                    Thread t = new Thread(curr);
                    t.start();
                    try {
                        t.sleep(1);
                    } catch (InterruptedException e) {
                        System.out.println("Unable to sleep");
                    }
                    reachableIPs = curr.getReachableIPs();
                }
            }
            try {
                Thread.sleep(curr.getTimeout());
            } catch (InterruptedException e) {
                System.out.println("Unable to sleep");

            }
        } else {

            System.out.println("subnetmask: " + subnetmask);
            String[] myIPsplit = myIP.split("[.]");
            String checkIP = myIPsplit[0] + "." + myIPsplit[1] + ".";
            ConnectionThread curr = null;

            host = "127.0.0.1";//checkIP + i + "." + j;
            if (host.equals(checkIP + 191 + "." + 254)) {
                System.out.println("Last host:" + host);
            }

            curr = new ConnectionThread(host, port, reachableIPs);
            Thread t = new Thread(curr);
            t.start();
            try {
                t.sleep(1);
            } catch (InterruptedException e) {
                System.out.println("Unable to sleep");
            }
            reachableIPs = curr.getReachableIPs();
            try {
                Thread.sleep(curr.getTimeout());
            } catch (InterruptedException e) {
                System.out.println("Unable to sleep");
            }
        }
        return reachableIPs;
    }

    public List<ServerThread> getServerThread() {
        return serverThreadList;
    }

    public void setServerThread(List<ServerThread> serverThread) {
        this.serverThreadList = serverThread;
    }

    public boolean getClient() {
        return client;
    }

    public void setClient(boolean client) {
        this.client = client;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }
}